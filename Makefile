#
COMPOSE = docker-compose

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
	$(COMPOSE) logs -f
