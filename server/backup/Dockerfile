FROM postgres:latest

RUN apt-get update && apt-get install -y python python-pip && pip install awscli
RUN apt-get remove -y python-pip && rm -rf /var/lib/apt/lists/*

COPY do_backup.sh /
RUN chmod +x /do_backup.sh

COPY task.sh /
RUN chmod +x /task.sh

# Create the log file to be able to run tail
RUN touch /var/log/backup.log

# Run the command on container startup
ENTRYPOINT /bin/bash -c /task.sh
