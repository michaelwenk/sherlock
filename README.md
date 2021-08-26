# WebCASE
Web services for Computer-Assisted Structure Elucidation (CASE).

The graphical user interface is available under https://github.com/michaelwenk/webcase-frontend.

## Core  Features
- Dereplication
- Elucidation
- Retrieval of previously generated results 

The dereplication is done by searching for hits in NMRShiftDB. 
PyLSD is used for structure generation. See [Dependencies](#dependencies) section.

## Download and Execution of pre-built Containers
This project uses Docker containers (https://www.docker.com) and starts them via docker-compose. Make sure that docker-compose is installed.

### Download
Clone this repository and change the directory:
 
    git clone https://github.com/michaelwenk/webcase.git
    cd webcase

Now pull all the containers needed for execution from Docker Hub:

     docker-compose -f docker-compose.yml -f docker-compose.publish.yml pull

### Start
To start them (in detached mode) use:

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml up -d

Note: It can take a few minutes until all services are available and registered.

### Stop
To stop the application use:

    docker-compose -f docker-compose.yml -f docker-compose.publish.yml down

## Self Compilation and Dependencies

### Compilation
CASEkit (https://github.com/michaelwenk/casekit) has to be downloaded and compiled beforehand.

Now add the jar file to the local Maven repository by following command:

(note: replace "PATH/TO/CASEKIT-JAR-WITH-DEPENDENCIES" by the path to previously built CASEkit jar):

    mvn install:install-file -Dfile=PATH/TO/CASEKIT-JAR-WITH-DEPENDENCIES -DgroupId=org.openscience -DartifactId=casekit -Dversion=1.0 -Dpackaging=jar

Clone this repository:

    git clone https://github.com/michaelwenk/webcase.git

Change the directory and build all the .jar files needed for this project using the build shell script:

    cd webcase
    sh buildJars.sh

### Dependencies
Some services rely on specific software or file dependencies which has to be downloaded and put into certain places.
#### PyLSD
For the structure generation part PyLSD (http://eos.univ-reims.fr/LSD/JmnSoft/PyLSD/) is needed.
PyLSD can be downloaded from http://eos.univ-reims.fr/LSD/JmnSoft/PyLSD/INSTALL.html.

Extract and rename the new PyLSD folder to "PyLSD", if needed.

Now put the PyLSD folder into 

    backend/webcase-pylsd/data/lsd/

In case custom filters are desired to use one can create a folder "filters" in

    backend/webcase-pylsd/data/lsd/

and put the custom filters there. The system will use them automatically.

For more details about LSD and defining substructures and filters see http://eos.univ-reims.fr/LSD/MANUAL_ENG.html#SSTR .

#### NMRShiftDB
For the dereplication, automatic hybridization detection und chemical shift prediction via HOSE codes the NMRShiftDB (https://nmrshiftdb.nmr.uni-koeln.de) is required.

Download the "nmrshiftdb2withsignals.sd" from https://sourceforge.net/projects/nmrshiftdb2/files/data/ and copy it into 

    backend/webcase-db-service-dataset/data/nmrshiftdb/

and rename the file to "nmrshiftdb.sdf". 

### Docker and Application Start/Stop
This project uses Docker containers (https://www.docker.com) and starts them via docker-compose. Make sure that docker-compose is installed.

#### Build
To build the container images use the following command:

    docker-compose -f docker-compose.yml -f docker-compose.production.yml build

#### Start 
To start this application (in detached mode) use

    docker-compose -f docker-compose.yml -f docker-compose.production.yml up -d

Note: It can take a few minutes until all services are available and registered.

#### Stop
To stop this application use

    docker-compose -f docker-compose.yml -f docker-compose.production.yml down

### Docker Container and Data Preparation/Persistence
The databases for datasets and hybridizations have to be filled when starting the application the first time.

After that procedure, the container database contents are stored in the "data/db" subdirectory of each "db-instance" service.
That enables the persistence of database content to access the data whenever the database services are restarting.

#### Dataset
For dataset creation and insertion use:

    curl -X POST -i 'http://localhost:8081/webcase-db-service-dataset/replaceAll?nuclei=13C'

This will fill-in datasets with 13C spectra only. If multiple nuclei are desired, 
then this could be done by adding them separated by comma, e.g. 13C, 15N: 

    curl -X POST -i 'http://localhost:8081/webcase-db-service-dataset/replaceAll?nuclei=13C,15N'

One can then check the number of datasets:

    curl -X GET -i 'http://localhost:8081/webcase-db-service-dataset/count' 

#### Hybridizations
As for datasets we need to insert the hybridization data and can decide which nuclei to consider:

    curl -X POST -i 'http://localhost:8081/webcase-db-service-hybridization/replaceAll?nuclei=13C'

To check the number of hybridization entries:

    curl -X GET -i 'http://localhost:8081/webcase-db-service-hybridization/count'

#### HOSE Codes
One needs to insert the HOSE code information too:

    curl -X POST -i 'http://localhost:8081/webcase-db-service-hosecode/replaceAll?nuclei=13C&maxSphere=6'

To check the number of HOSE code entries:

    curl -X GET -i 'http://localhost:8081/webcase-db-service-hosecode/count'


All services and database contents should be available now.


