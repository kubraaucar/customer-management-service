# Frontend image: builds the React app, then serves the files with nginx.
#   docker build -t customer-management-frontend .

# ---- 1. Build ----
FROM node:26-alpine AS build
WORKDIR /app

# Install packages first; reused as long as package*.json do not change.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# Backend address baked into the app. Empty means "same address": nginx below forwards /api to the backend.
ARG VITE_API_URL=
RUN npm run build

# ---- 2. Serve ----
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
