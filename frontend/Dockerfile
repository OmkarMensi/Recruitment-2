FROM node as build
WORKDIR /app
COPY package*.json .
RUN npm install
COPY . .
RUN npm run build

#stage-2

FROM node
WORKDIR /app
COPY --from=build /app ./
EXPOSE 3000
CMD ["npm","start"]
