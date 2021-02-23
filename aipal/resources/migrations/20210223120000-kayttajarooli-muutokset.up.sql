DROP TABLE kayttajarooli CASCADE;
--;;
DELETE FROM rooli_organisaatio WHERE kayttaja NOT IN ('JARJESTELMA', 'INTEGRAATIO', 'KONVERSIO', 'VASTAAJA');
--;;
ALTER TABLE rooli_organisaatio RENAME COLUMN rooli TO kayttooikeus;

