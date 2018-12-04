#!/bin/bash

mvn package appassembler:assemble

ln -s $(realpath target/appassembler/bin/jytdl) .

