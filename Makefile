dockerComposeUp:
	docker compose -f docker/docker-compose.yml up -d
dockerComposeDown:
	docker compose -f docker/docker-compose.yml down
