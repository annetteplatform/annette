FROM postgres:15.3
COPY init.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/init.sh
