docker compose -f docker-compose.yml -f compose.dev.yml up -d postgres-customer postgres-ledger redis kafka

docker compose down postgres-customer -v

docker compose down -v

docker compose -f docker-compose.yml -f compose.dev.yml up -d postgres-customer postgres-ledger redis kafka