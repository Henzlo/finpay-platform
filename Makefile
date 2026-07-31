.PHONY: infra-up infra-down infra-clean logs ps kafka-topics help

COMPOSE ?= docker-compose

help:
	@echo "FinPay infrastructure shortcuts"
	@echo "  make infra-up      Start all infra services in the background"
	@echo "  make infra-down    Stop containers (keeps volumes)"
	@echo "  make infra-clean   Stop containers and delete volumes"
	@echo "  make logs          Tail compose logs"
	@echo "  make ps            Show compose status"
	@echo "  make kafka-topics  List Kafka topics"

infra-up:
	$(COMPOSE) up -d

infra-down:
	$(COMPOSE) down

infra-clean:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

kafka-topics:
	docker exec finpay-kafka kafka-topics --bootstrap-server localhost:9092 --list
