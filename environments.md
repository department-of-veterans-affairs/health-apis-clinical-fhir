| DSVA                 | DVP         | MPI            | IAM     | Vista        | DQ DB    |
|----------------------|-------------|----------------|---------|--------------|----------|
| `dev-api.va.gov`     | QA          | DISABLED (DEV) | INT     | Tampa Docker | CDW      |
| -                    | UAT         |                | -       | -            | CDW      |
| -                    | Staging     | Pre-Prod       | -       | Loma Linda   | CDW      |
| -                    | Staging-Lab | DISABLED (SQA) | -       | Tampa Docker | Synth DB |
| `staging-api.va.gov` | -           | -              | INT     | -            | -        |
| `sandbox-api.va.gov` | Lab         | DISABLED (SQA) | PREPROD | Tampa Docker | Synth DB |
| `api.va.gov`         | Production  | Prod           | PROD    | Loma Linda   | CDW      |

### `DISABLED MPI INTERACTION`

Under ideal circumstances, MPI is consulted when searching patient record to determine the Vista instances relevant for
a particular patient. Environments with _Tampa Docker_ instances for Vista have interaction with MPI disabled. In these
environments, MPI will not have information on the patient requested, or return Vista sites that are not Tampa resulting
in no data found. Instead all known vistas are queried, which happens to be the one dockerized Tampa
instance.

### `Tampa Docker`
Environments using the `Tampa Docker` Vista environment are subject the follow characteristics:
- Data set is well known and change controlled
- No user access to running Vista instances, e.g. no CPRS
- Limited variability in data, e.g., COVID-19 data is not available
- Limited patients that do not necessarily match Data Query records

_Vista FHIR Query_, the API in front of Vista data, provides an _Alternate IDs_ capability in lower environments that allows one test patient ID to be mapped to different test patient ID. This provides data compatibility for a limited set of patients across Data Query/Synth DB and Vista FHIR Query/Tampa Docker. 

### `dev-api-va.gov`

- No SLA, volatile, subject to breakages
- Disabled MPI interaction



![environments](src/plantuml/environments.png)