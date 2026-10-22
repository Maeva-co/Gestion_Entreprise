# Gestion_Entreprise

## creation de la base :
### connextion à psql en tant que postgres
```bash
sudo -u postgres psql
```

### Requete de creation utilisateur rh_user:
```sql 
CREATE USER rh_user WITH PASSWORD 'Rh2026!';
ALTER USER rh_user CREATEDB;
\q
```

### Creer de base :
```bash
sudo -u postgres createdb -O rh_user gestion_rh
```

### creer les tables :
```bash
PGPASSWORD='Rh2026!' psql -h localhost -U rh_user -d gestion_rh -v ON_ERROR_STOP=1 -f bloc1.sql
```
**verification** : 
```bash 
PGPASSWORD='Rh2026!' psql -h localhost -U rh_user -d gestion_rh -c "\dt"
```

### insertion des données de test :
```bash
PGPASSWORD='RhTest2026!' psql -h localhost -U rh_user -d gestion_rh -v ON_ERROR_STOP=1 -f donnebloc1.sql
```

**verification**
```bash
PGPASSWORD='RhTest2026!' psql -h localhost -U rh_user -d gestion_rh -c "SELECT * FROM type_demande_recrutement;"
```


### Reinitialiser la base :
```bash
sudo -u postgres dropdb --if-exists --force gestion_rh

sudo -u postgres createdb -O rh_user gestion_rh

PGPASSWORD='Rh2026!' psql -h localhost -U rh_user -d gestion_rh -v ON_ERROR_STOP=1 -f bloc1.sql

PGPASSWORD='Rh2026!' psql -h localhost -U rh_user -d gestion_rh -v ON_ERROR_STOP=1 -f donnebloc1.sql

```

### vider les tables :
```bash 
PGPASSWORD='Rh2026!' psql -h localhost -U rh_user -d gestion_rh -v ON_ERROR_STOP=1 -c "
DO \$\$
DECLARE r RECORD;
BEGIN
  FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
  END LOOP;
END \$\$;"
```