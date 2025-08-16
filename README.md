# Order Management System

## Описание
REST API приложение на Spring Boot для управления заказами и клиентами с поддержкой PostgreSQL.

## Технологии
- Spring Boot
- Spring Data JPA (Hibernate)
- PostgreSQL
- Docker Compose
- Swagger/OpenAPI

## Установка и запуск

### Предварительные требования
- Java 21+
- Maven 3.6+
- Docker Compose

### Запуск с Docker Compose
```bash
# Сборка приложения
mvn clean package

# Запуск всех сервисов
docker-compose up -d
```

## API Документация
После запуска приложения доступна по адресу: http://localhost:8080/swagger-ui.html

## Основные эндпоинты

### Клиенты
- `POST /api/clients` - Создание клиента
- `GET /api/clients` - Получение всех клиентов
- `GET /api/clients/search` - Поиск клиента по ключевому слову
- `GET /api/clients/{id}` - Получение клиента по ID
- `PUT /api/clients/{id}` - Обновление клиента
- `POST /api/clients/{id}/deactivate` - Деактивация клиента
- `GET /api/clients/{id}/profit` - Прибыль клиента
- `GET /api/clients/profit-range?minProfit={min}&maxProfit={max}` - Клиенты в диапазоне прибыли

### Заказы
- `POST /api/orders` - Создание заказа
- `GET /api/orders` - Получение всех заказов
- `GET /api/orders/{id}` - Получение заказа по ID
- `GET /api/orders/client/{clientId}` - Заказы клиента
- `GET /api/orders/supplier/{supplierId}` - Заказы поставщика
- `GET /api/orders/consumer/{consumerId}` - Заказы потребителя
