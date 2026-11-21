#
COMPOSE = docker compose

#installer docker 
install: 
		chmod +x install_docker.sh
		./install_docker.sh

#run Project
run:   
	cd backend && mvn clean package -DskipTests
	$(COMPOSE) up --build -d

#Pour nettoyage
clean:
	$(COMPOSE) down --rmi all --volumes 
	rm -rf backend/target

#Pour logs
logs:
	$(COMPOSE) logs -fA

#Lancer les tests (backend + frontend)
test: test-back test-front

test-back:
	cd backend && mvn -B test

test-front:
	cd frontend && npm ci && npx ng test --watch=false
