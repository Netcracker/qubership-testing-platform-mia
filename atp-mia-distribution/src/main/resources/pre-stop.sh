#!/usr/bin/env sh

xargs -rt -a /atp-mia/application.pid kill -SIGTERM
sleep 29
