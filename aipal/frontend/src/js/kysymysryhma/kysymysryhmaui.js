// Copyright (c) 2014 The Finnish National Board of Education - Opetushallitus
//
// This program is free software:  Licensed under the EUPL, Version 1.1 or - as
// soon as they will be approved by the European Commission - subsequent versions
// of the EUPL (the "Licence");
//
// You may not use this work except in compliance with the Licence.
// You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// European Union Public Licence for more details.

'use strict';

angular.module('kysymysryhma.kysymysryhmaui', ['ngRoute', 'rest.kysymysryhma',
                                               'yhteiset.palvelut.i18n',
                                               'yhteiset.palvelut.ilmoitus'])

  .config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/kysymysryhmat', {
        controller: 'KysymysryhmatController',
        templateUrl: 'template/kysymysryhma/kysymysryhmat.html'
      })
      .when('/kysymysryhma/uusi', {
        controller: 'UusiKysymysryhmaController',
        templateUrl: 'template/kysymysryhma/uusi.html'
      });
  }])

  .controller('KysymysryhmatController', ['$scope', 'Kysymysryhma',
                                          function($scope, Kysymysryhma) {
    $scope.latausValmis = false;
    Kysymysryhma.haeKaikki().success(function(kysymysryhmat){
      $scope.kysymysryhmat = kysymysryhmat;
      $scope.latausValmis = true;
    });
  }])

  .controller('UusiKysymysryhmaController', ['$scope', '$window', 'Kysymysryhma',
                                             'i18n', 'ilmoitus',
                                             function($scope, $window,
                                                 Kysymysryhma, i18n, ilmoitus){
    $scope.kysely = {};
    $scope.peruuta = function(){
      $window.location.hash = '/kysymysryhmat';
    };
    $scope.luoUusi = function(){
      Kysymysryhma.luoUusi($scope.kysely)
      .success(function(){
        $window.location.hash = '/kysymysryhmat';
        ilmoitus.onnistuminen(i18n.hae('kysymysryhma.luonti_onnistui'));
      })
      .error(function(){
        ilmoitus.virhe(i18n.hae('kysymysryhma.luonti_epaonnistui'));
      });
    };
  }]);
