# Teil 1: Angular-App mit Node bauen (npm, Angular CLI, TypeScript)
FROM node:26 AS build
WORKDIR /app
# Zuerst nur die Abhängigkeits-Files kopieren, damit "npm ci" im Cache bleibt, solange sie sich nicht ändern
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Teil 2: Nur die statischen Dateien mit nginx ausliefern (kein Node im fertigen Image)
FROM nginx:1.31-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
