#
COMPOSE = docker compose
.PHONY: install test test-back test-front run smoke logs stop clean

#installer docker 
install: 
		chmod +x install_docker.sh
		./install_docker.sh

#run Project
run:   
	$(COMPOSE) up --build -d


#Pour logs
logs:
	$(COMPOSE) logs -fA

#Lancer les tests (backend + frontend)
test: test-back test-front

test-back:
	cd backend && mvn -B test

test-front:
	cd frontend && npm ci && npx ng test --watch=false

stop:
	$(COMPOSE) down

clean:
	$(COMPOSE) down --rmi local --volumes
	rm -rf backend/target

smoke:
	./smoke_test.sh
