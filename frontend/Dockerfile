# Etape 1 : compiler l'application Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build
#checkov:skip=CKV_DOCKER_2:Healthcheck défini dans docker-compose.yml.
# Etape 2 : servir les fichiers compilés avec Nginx
# mainline : nginx.org y publie les correctifs de sécurité en premier
# (stable-alpine restait en 1.30.6, vulnérable)
FROM nginxinc/nginx-unprivileged:mainline-alpine
# apk a besoin de root pour mettre à jour les paquets Alpine (musl, curl, libxml2...)
USER root
RUN apk upgrade --no-cache
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]