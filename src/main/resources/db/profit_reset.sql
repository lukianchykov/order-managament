BEGIN;

DROP TABLE IF EXISTS temp_client_profits;

CREATE TEMP TABLE temp_client_profits AS
SELECT
    c.id as client_id,
    c.name as client_name,
    COALESCE(c.profit, 0) as current_profit
FROM clients c
WHERE c.active = true
  AND COALESCE(c.profit, 0) != 0;

SELECT
    COUNT(*) as clients_to_process,
    SUM(CASE WHEN current_profit > 0 THEN 1 ELSE 0 END) as positive_profit_clients,
    SUM(CASE WHEN current_profit < 0 THEN 1 ELSE 0 END) as negative_profit_clients,
    SUM(current_profit) as total_profit_before_reset
FROM temp_client_profits;

INSERT INTO clients (name, email, address, active, profit, created_at, updated_at)
VALUES (
           'SYSTEM_PROFIT_RESET',
           'system.reset.' || EXTRACT(EPOCH FROM NOW())::BIGINT || '@internal.system',
           'System Generated for Profit Reset',
           true,
           0,
           NOW(),
           NOW()
       )
ON CONFLICT (email) DO NOTHING;

DO $$
    DECLARE
        system_client_id BIGINT;
        client_record RECORD;
        order_name TEXT;
        processed_count INTEGER := 0;
        total_reset_amount NUMERIC := 0;
    BEGIN
        SELECT id INTO system_client_id
        FROM clients
        WHERE name = 'SYSTEM_PROFIT_RESET'
          AND email LIKE 'system.reset.%@internal.system'
        ORDER BY created_at DESC
        LIMIT 1;

        IF system_client_id IS NULL THEN
            RAISE EXCEPTION 'Failed to create or find system client';
        END IF;

        RAISE NOTICE 'Using system client ID: %', system_client_id;

        FOR client_record IN
            SELECT client_id, client_name, current_profit
            FROM temp_client_profits
            ORDER BY client_id
            LOOP
                order_name := 'PROFIT_RESET_' || client_record.client_id || '_' ||
                              EXTRACT(EPOCH FROM NOW())::BIGINT || '_' || processed_count;

                IF client_record.current_profit > 0 THEN
                    INSERT INTO orders (
                        name,
                        supplier_id,
                        consumer_id,
                        price,
                        processing_start_time,
                        processing_end_time,
                        created_at
                    )
                    VALUES (
                               order_name,
                               system_client_id,
                               client_record.client_id,
                               client_record.current_profit,
                               NOW(),
                               NOW(),
                               NOW()
                           );

                    UPDATE clients
                    SET profit = COALESCE(profit, 0) - client_record.current_profit,
                        updated_at = NOW()
                    WHERE id = client_record.client_id;

                    UPDATE clients
                    SET profit = COALESCE(profit, 0) + client_record.current_profit,
                        updated_at = NOW()
                    WHERE id = system_client_id;

                ELSE
                    INSERT INTO orders (
                        name,
                        supplier_id,
                        consumer_id,
                        price,
                        processing_start_time,
                        processing_end_time,
                        created_at
                    )
                    VALUES (
                               order_name,
                               client_record.client_id,
                               system_client_id,
                               ABS(client_record.current_profit),
                               NOW(),
                               NOW(),
                               NOW()
                           );

                    UPDATE clients
                    SET profit = COALESCE(profit, 0) + ABS(client_record.current_profit),
                        updated_at = NOW()
                    WHERE id = client_record.client_id;

                    UPDATE clients
                    SET profit = COALESCE(profit, 0) - ABS(client_record.current_profit),
                        updated_at = NOW()
                    WHERE id = system_client_id;

                END IF;

                processed_count := processed_count + 1;
                total_reset_amount := total_reset_amount + ABS(client_record.current_profit);

                RAISE NOTICE 'Reset profit for client % (ID: %): % -> 0',
                    client_record.client_name, client_record.client_id, client_record.current_profit;
            END LOOP;

        RAISE NOTICE 'Profit reset completed for % clients, total amount: %',
            processed_count, total_reset_amount;
    END $$;

SELECT
    'VERIFICATION' as status,
    COUNT(*) as total_active_clients,
    COUNT(CASE WHEN COALESCE(c.profit, 0) = 0 THEN 1 END) as zero_profit_clients,
    COUNT(CASE WHEN COALESCE(c.profit, 0) != 0 THEN 1 END) as non_zero_profit_clients,
    SUM(COALESCE(c.profit, 0)) as total_profit_after_reset
FROM clients c
WHERE c.active = true
  AND c.name != 'SYSTEM_PROFIT_RESET';

SELECT
    c.id,
    c.name,
    c.email,
    COALESCE(c.profit, 0) as current_profit
FROM clients c
WHERE c.active = true
  AND c.name != 'SYSTEM_PROFIT_RESET'
  AND COALESCE(c.profit, 0) != 0
ORDER BY c.profit DESC;

SELECT
    'SYSTEM CLIENT INFO' as info,
    c.id,
    c.name,
    c.email,
    COALESCE(c.profit, 0) as system_client_profit,
    c.created_at
FROM clients c
WHERE c.name = 'SYSTEM_PROFIT_RESET'
ORDER BY c.created_at DESC
LIMIT 1;

COMMIT;