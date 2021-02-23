create table kayttajarooli(
    roolitunnus varchar(32) NOT NULL PRIMARY KEY,
    kuvaus varchar(200),
    luotuaika TIMESTAMP WITH TIME ZONE NOT NULL,
    muutettuaika TIMESTAMP WITH TIME ZONE NOT NULL
);
--;;
GRANT SELECT ON kayttajarooli TO PUBLIC;
--;;
ALTER TABLE rooli_organisaatio ADD CONSTRAINT rooli_organisaatio_rooli_fkey REFERENCES kayttajarooli(roolitunnus);
--;;
INSERT INTO kayttajarooli VALUES ('YLLAPITAJA', 'Ylläpitäjäroolilla on kaikki oikeudet', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('OPH-KATSELIJA', 'Opetushallituksen katselija', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('OPL-VASTUUKAYTTAJA', 'Oppilaitoksen vastuukayttaja', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('OPL-KATSELIJA', 'Oppilaitoksen katselija', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('OPL-KAYTTAJA', 'Oppilaitoksen normaali käyttäjä (opettaja)', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('KATSELIJA', 'Yleinen katselijarooli erityistarpeita varten', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
INSERT INTO kayttajarooli VALUES ('AIPAL-VASTAAJA', 'Vastaajasovelluksen käyttäjän rooli', '2021-02-09 17:03:49.954802+02', '2021-02-09 17:03:49.954802+02');
--;;
ALTER TABLE rooli_organisaatio RENAME COLUMN kayttooikeus TO rooli;

