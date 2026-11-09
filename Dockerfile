# Use the official PostgreSQL 16.4 image as the base
FROM postgres:18.6-trixie

# Install build dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    postgresql-server-dev-18

# Clone and build pgvector
RUN git clone https://github.com/pgvector/pgvector.git \
    && cd pgvector \
    && make \
    && make install

# Clean up
RUN apt-get remove -y build-essential git postgresql-server-dev-18\
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/* \
#docker run \
 #  --name postgres-vector \
 #  -e POSTGRES_PASSWORD=mysecretpassword \
 #  -d \
 #  -p 5432:5432 \
 #  postgres-pgvector