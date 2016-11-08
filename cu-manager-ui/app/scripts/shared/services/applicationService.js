/*
* LICENCE : CloudUnit is available under the Affero Gnu Public License GPL V3 : https://www.gnu.org/licenses/agpl-3.0.html
*     but CloudUnit is licensed too under a standard commercial license.
*     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
*     If you are not sure whether the GPL is right for you,
*     you can always test our software under the GPL and inspect the source code before you contact us
*     about purchasing a commercial license.
*
*     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
*     or promote products derived from this project without prior written permission from Treeptik.
*     Products or services derived from this software may not be called "CloudUnit"
*     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
*     For any questions, contact us : contact@treeptik.fr
*/

(function () {
    'use strict';

/**
* @ngdoc service
* @name webuiApp.ApplicationService
* @description
* # ApplicationService
* Factory in the webuiApp.
*/
angular
.module ( 'webuiApp' )
.factory ( 'ApplicationService', ApplicationService );

ApplicationService.$inject = [
'$resource',
'$http',
'$interval',
'traverson',
'$q'
];


function ApplicationService ( $resource, $http, $interval, traverson, $q ) {


    var Application;
    Application = $resource ( 'applications/:id', { id: '@name' } );

    var traversonService = traverson
        .from('http://localhost:9000/applications')
        .json()
        .withRequestOptions({ headers: { 'Content-Type': 'application/json' } });

    return {
        about: about,
        list: list,
        create: create,
        start: start,
        stop: stop,
        isValid: isValid,
        remove: remove,
        findByName: findByName,
        listContainers: listContainers,
        createAlias: createAlias,
        removeAlias: removeAlias,
        createPort: createPort,
        removePort: removePort,
        openPort: openPort,
        restart: restart,
        init: init,
        state: {},
        stopPolling: stopPolling
    };


///////////////////////////////////////////////////////

function assignObject(target) {
    target = Object(target);
    for (var index = 1; index < arguments.length; index++) {
        var source = arguments[index];
        if (source != null) {
            for (var key in source) {
                if (Object.prototype.hasOwnProperty.call(source, key)) {
                    target[key] = source[key];
                }
            }
        }
    }
    return target;
};

// @TODO
// A propos du manager
function about () {
    return $http.get ( 'about' ).then ( function ( response ) {
        return angular.copy ( response.data );
    } )
}

// @TODO remove mock
// Liste des applications
function list () {

    var res = 
        [
        {
            "id":2,
            "name":"plop",
            "displayName":"plop",
            "cuInstanceName":"DEV",
            "origin":null,
            "status":"START",
            "date":"2016-11-07 15:03",
            "user":{
                "id":1,
                "login":"johndoe",
                "firstName":"John",
                "lastName":"Doe",
                "organization":"admin",
                "signin":"2013-08-22 07:22",
                "lastConnection":null,
                "email":"johndoe.doe@gmail.com",
                "status":1,
                "role":{
                "id":1,
                "description":"ROLE_ADMIN"
            }
        },
        "modules":[
        ],
        "server":{
            "id":3,
            "startDate":"2016-11-07 15:03",
            "name":"dev-johndoe-plop-tomcat-7",
            "containerID":"e8bbc8d0dc73",
            "memorySize":null,
            "containerIP":"172.17.0.8",
            "status":"START",
            "image":{
            "id":11,
            "name":"tomcat-7",
            "path":"cloudunit/tomcat-7",
            "displayName":"Tomcat 7.0.70",
            "status":null,
            "imageType":"server",
            "managerName":"",
            "prefixEnv":"tomcat",
            "exposedPorts":null,
            "prefixId":-868129468,
            "imageSubType":null,
            "moduleEnvironmentVariables":null
        },
        "internalDNSName":null,
        "sshPort":null,
        "jvmMemory":512,
        "jvmOptions":"",
        "jvmRelease":"jdk1.8.0_25",
        "managerLocation":"http://manager-plop-johndoe-admin.cloudunit.dev/manager/html?",
        "containerFullID":null
        },
        "deployments":[
        ],
        "aliases":[
        ],
        "suffixCloudUnitIO":".cloudunit.dev",
        "domainName":".cloudunit.dev",
        "managerIp":"192.168.50.4:4243",
        "managerPort":null,
        "jvmRelease":"jdk1.8.0_25",
        "deploymentStatus":"NONE",
        "contextPath":null,
        "portsToOpen":[
        ],
        "location":"http://plop-johndoe-admin.cloudunit.dev",
        "aclone":false
        }
    ];
    return $http.get ( 'applications' ).then ( function ( response ) {
        return angular.copy ( res );
    } )
}

// Creation d'une application
function create ( name, serverType ) {
    var payload = {
        name: name,
        serverType: serverType
    };

    return toPromise(traversonService.post(  payload ).result);
}

function toPromise(promise) {
    var q = $q.defer();

    promise
        .then(function (response) {
            if(response.status === undefined || (response.status>= 200 && response.status < 300)) {
                 if(response.body) {
                     q.resolve(JSON.parse(response.body));
                 } else {
                     q.resolve('');
                 }
            } else {
                 if(response.body) {
                     q.reject(JSON.parse(response.body));
                 } else {
                     q.reject('');
                 }
            }
        });

    return q.promise;
}

// Creation d'une application
// function create ( applicationName, serverName ) {
//     var output = {};
//     output.applicationName = applicationName;
//     output.serverName = serverName;

//     return Application.save ( JSON.stringify ( output ) ).$promise;
// }

// Démarrer une application
function start ( applicationName ) {
    return toPromise(traversonService
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "start")].href')
        .post()
        .result);

    // var output = {};
    // output.applicationName = applicationName;
    // var Application = $resource ( 'applications/start' );
    // return Application.save ( JSON.stringify ( output ) ).$promise;
}

// Démarrer une application
function restart ( applicationName ) {

   return toPromise(traversonService
        .newRequest()
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "restart")].href')
        .post()
        .result);
}

// Arrêter une application
function stop ( applicationName ) {

   return toPromise(traversonService
        .newRequest()
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "stop")].href')
        .post()
        .result);
}

// @TODO
// Teste la validite d'une application avant qu'on puisse la creer
function isValid ( applicationName, serverName ) {
    var validity = $resource ( 'applications/verify/' + applicationName + '/' + serverName );
    return validity.get ().$promise;
}

// Suppression d'une application
function remove ( applicationName ) {
    console.log('remove');
       traversonService
        .newRequest()
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "self")].href')
        .delete();
}

// @TODO return image property
// Récupération d'une application selon son nom
function findByName ( applicationName ) {
    var self = this;

   return toPromise(traversonService
        .newRequest()
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "self")].href')
        .get()
        .result)
            .then ( function ( response ) {
                return assignObject(self.state, response);
            } ).catch ( function () {
                stopPolling.call ( self );
            } );
    // return $http.get ( 'applications/' + applicationName ).then ( function ( response ) {
    //     return assignObject(self.state, response.data);
    // } ).catch ( function () {
    //     stopPolling.call ( self );
    // } )
}
// @TODO
function init ( applicationName ) {
    var self = this;
    if ( !self.timer ) {
        self.timer = pollApp.call ( self, applicationName );
    }
    return findByName.call ( self, applicationName ).then ( function ( response ) {
        self.state = response;
    } );
}
// @TODO
function pollApp ( applicationName ) {
    var self = this;
    return $interval ( function () {
        findByName.call ( self, applicationName ).then ( function ( response ) {
            return self.state = response;
        } );
    }, 2000 )
}
// @TODO
function stopPolling () {
    if ( this.timer ) {
        $interval.cancel ( this.timer );
        this.timer = null;
    }
}

// @TODO
// Liste de toutes les container d'une application en fonction du type server/module
function listContainers ( applicationName ) {
    var container = $resource ( 'application/:applicationName/containers' );
    return container.query ( { applicationName: applicationName } ).$promise;
}

// Gestion des alias
// @TODO
function createAlias ( applicationName, alias ) {
    
    var payload = {
        alias: alias
    };

    return toPromise(traversonService
        .newRequest()
        .follow('$.content[?(@.name == "' + applicationName + '")].links[?(@.rel == "aliases")].href')
        .post(  payload )
        .result);
}
// @TODO
function removeAlias ( applicationName, alias ) {
    return $http.delete ( 'application/' + applicationName + '/alias/' + alias );
}


// Gestion des ports
// @TODO
function createPort ( applicationName, number, nature, isQuickAccess ) {
    var data = {
        applicationName: applicationName,
        portToOpen: number,
        portNature: nature,
        portQuickAccess: isQuickAccess
    };
    return $http.post ( 'application/ports', data );
}
// @TODO
function removePort ( applicationName, number ) {
    return $http.delete ( 'application/' + applicationName + '/ports/' + number );
}
// @TODO
 function openPort(moduleID, statePort, portInContainer) {
    var data = {
        publishPort: statePort
    };

    var dir = $resource ( '/module/:moduleID/ports/:portInContainer' ,
    { 
        moduleID: moduleID,
        portInContainer: portInContainer
    },
    { 
        'update': { 
            method: 'PUT',
            transformResponse: function ( data, headers ) {
                var response = {};
                response = JSON.parse(data);
                return response;
            }
        }
    }
    );
    return dir.update( { }, data ).$promise;
}

}
}) ();
