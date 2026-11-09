#
COMPOSE = docker-compose

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
