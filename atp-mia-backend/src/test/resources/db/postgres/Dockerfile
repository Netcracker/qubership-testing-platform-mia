FROM artifactory-service-address/path-to-java-image

ENV POSTGRES_USER=mia \
POSTGRES_PASSWORD=mia \
POSTGRES_DB=mia

RUN echo "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" > /docker-entrypoint-initdb.d/create_extension.sql

EXPOSE 5432
