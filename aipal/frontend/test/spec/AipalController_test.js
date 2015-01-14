// Copyright (c) 2015 The Finnish National Board of Education - Opetushallitus
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

// HUOM! aipal-moduulissa määritellään $exceptionHandler, joka lähettää
// kaikki poikkeukset palvelimelle. Tämä sotkee virheiden tulostuksen
// testeissä. Kommentoi ".factory('$exceptionHandler', ...)" pois aipal.js:stä,
// niin näet virheet konsolissa.

describe('AipalController', function() {

  var $scope;
  var $controller;
  var $httpBackend;

  beforeEach(module('aipal'));

  beforeEach(module(function($provide){
    $provide.value('i18n', {hae: function(){return '';}});
  }));

  beforeEach(inject(function($rootScope, _$controller_, _$httpBackend_) {
    $scope = $rootScope.$new();
    $controller = _$controller_;
    $httpBackend = _$httpBackend_;
    $httpBackend.whenGET(/.*\.html/).respond(200);
  }));

  afterEach(function(){
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  function alustaController() {
    $controller('AipalController', {$scope: $scope});
    $scope.form = { $setPristine: function() {} };
  }

  it('ilman impersonointia näyttää käyttäjän oman nimen', function(){
    $httpBackend
    .whenGET(/api\/kayttaja.*/)
    .respond(200, {etunimi: 'Aku',
                   sukunimi: 'Ankka',
                   impersonoitu_kayttaja: ''});
    alustaController();
    $httpBackend.flush();
    expect($scope.currentuser).toEqual('Aku Ankka');
  });

  it('impersonoitaessa näyttää impersonoidun käyttäjän nimen', function(){
    $httpBackend
    .whenGET(/api\/kayttaja.*/)
    .respond(200, {etunimi: 'Aku',
                   sukunimi: 'Ankka',
                   impersonoitu_kayttaja: 'Mikki Hiiri'});
    alustaController();
    $httpBackend.flush();
    expect($scope.currentuser).toEqual('Mikki Hiiri');
  });

});
