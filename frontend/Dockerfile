# syntax=docker/dockerfile:1.6

# Stage 1: build the Vite bundle.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: nginx serves the static bundle.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Proxies /api and /ws to the backend, SPA fallback for everything else.
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
