# Etape 1 : compiler l'application Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build
#checkov:skip=CKV_DOCKER_2:Healthcheck défini dans docker-compose.yml.
# Etape 2 : servir les fichiers compilés avec Nginx
FROM nginxinc/nginx-unprivileged:stable-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# Config Nginx pour gérer les routes Angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
