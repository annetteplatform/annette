FROM postgres:15.3
COPY init.sh /docker-entrypoint-initdb.d/
COPY keycloak.sql /tmp
RUN chmod +x /docker-entrypoint-initdb.d/init.sh
