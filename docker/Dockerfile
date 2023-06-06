################## BASE IMAGE ######################
FROM nfcore/base

################## METADATA ######################
LABEL authors="David Bouyssie" \
      maintainer=" David Bouyssie" \
      description="Docker image containing the latest release of mzdbtools"

################## INSTALLATION ######################

### Install some required unix tools
RUN apt-get update; apt-get install -y curl libgomp1 zip unzip wget; apt-get clean

### Install Mono (required for thermo2mzdb execution)
RUN apt install -y mono-complete 

### Install mzDB Tools
RUN mkdir mzdbtools ; cd mzdbtools; \
    wget https://github.com/mzdb/mzdb4s/releases/download/0.4.5/mzdbtools_0.4.5_linux_x64.zip; \
    unzip mzdbtools_0.4.5_linux_x64.zip; cd ..; \
    chmod a+x /mzdbtools/mzdbtools; \
    chmod a+x /mzdbtools/run_thermo2mzdb.sh; ln -s /mzdbtools/run_thermo2mzdb.sh /usr/bin/thermo2mzdb; \
    chmod a+x /mzdbtools/run_tdf2mzdb.sh; ln -s /mzdbtools/run_tdf2mzdb.sh /usr/bin/tdf2mzdb; \
    chmod a+x /mzdbtools/run_mzdb2mgf.sh; ln -s /mzdbtools/run_mzdb2mgf.sh /usr/bin/mzdb2mgf

### Update LD_LIBRARY_PATH to load .so files from :/mzdbtools/lib/
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/mzdbtools/lib/

WORKDIR /mzdbtools

ENTRYPOINT ["/bin/bash","/usr/bin/thermo2mzdb"]

