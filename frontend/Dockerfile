FROM nginx:stable-alpine

# Copie des fichiers compilés Angular vers Nginx
COPY dist/votre-projet-name /usr/share/nginx/html

# Copie d'une config Nginx pour gérer les routes Angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]