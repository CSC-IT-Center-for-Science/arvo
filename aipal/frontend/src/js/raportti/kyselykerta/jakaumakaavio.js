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

angular.module('raportti.kyselykerta.jakaumakaavio', ['raportti.kyselykerta.kaavioapurit'])
  .directive('jakaumaKaavio', ['kaavioApurit', function(kaavioApurit) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        jakauma: '='
      },
      templateUrl: 'template/raportti/jakaumaKaavio.html',
      link: function(scope) {
        var asetukset = {
          maksimitilaOtsikolle: 300,
          palkinMaksimiPituus: 480,
          otsikoidenSisennys: 50,
          tekstinMaksimiPituus: 80
        };

        _.assign(scope, _.pick(kaavioApurit, ['jaaTeksti', 'maksimi', 'lukumaaratYhteensa', 'palkinVari']));
        scope.otsikoilleTilaa = _.partial(kaavioApurit.otsikoilleTilaa, asetukset);
        scope.palkinPituus = _.partial(kaavioApurit.palkinPituus, asetukset);
        scope.otsikot = [
          {x: 0, teksti: ''},
          {x: 0.25, teksti: '25%'},
          {x: 0.5, teksti: '50%'},
          {x: 0.75, teksti: '75%'},
          {x: 1.0, teksti: '100%'}
        ];
      }
    };
  }]);
