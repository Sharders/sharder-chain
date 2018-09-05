# Sharder docker image
#
# to use:
#
# 1. make sure the right version docker be installed
# 2. clone the git repo from github and git.sharder.org
# 3. build the container with ```docker build -t sharder .```
# 4. run the created sharder container with ```docker run -d -p 127.0.0.1:8215:8215 -p 8218:8218 sharder``` or docker compose
# 5. inspect with docker logs (image hash, find out with docker ps, or assign a name)

FROM jeanblanchard/java:jdk-8u181
LABEL version="0.0.1"
# start off with standard ubuntu images
# run and compile sharder
RUN mkdir /sharder
ADD . /sharder
# repo has
ADD docker/docker_start.sh /docker_start.sh
# set sharder to listen on all interfaces
RUN echo 'sharder.allowedBotHosts=*' >> /sharder/conf/sharder.properties
RUN echo 'sharder.apiServerHost=0.0.0.0' >> /sharder/conf/sharder.properties
RUN chmod +x /docker_start.sh

RUN cd /sharder; ./compile.sh
# both sharder ports get exposed
EXPOSE 8215 8218 8001 9001 8099
CMD ["/docker_start.sh"]