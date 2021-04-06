# Token validation V2

```
{
  data: {
    id: string,
    type: string,
    attributes: {
      ver: number,
      jti: string,
      iss: string,
      aud: string,
      iat: number,
      exp: number,
      cid: string,
      uid: string | null,
      scp: [
        string
      ],
      sub: string,
      act: {
        icn: string | null,
        npi: string | null,
        sec_id: string | null,
        vista_id: string | null
      },
      launch: {
        patient: string | null,
        sta3n: string | null
      }
    }
  }
}
```

## Authentication Steps

- Verify `Authorization` header exists (else `401`)
- Verify `Authorization` header has a value of `Bearer ${TOKEN}` (else `403`)
  - If `TOKEN == ${staticAccessToken}` perform _Static Access Token Validation_
  - If `TOKEN != ${staticAccessToken}` perform _Standard Validation_

### Standard Validation

> Standard validation applies the following logic based on the type of request.
> 
> _Patient_
> - Must have patient-level scopes that match resource requested
> - Can only access their data
> 
> _User on behalf of a Patient_
> - Must have patient-level scopes that match resource requested
> - Can only access data for the patient in their context
>
> _User_
> - Must have patient-level scopes that match resource requested
> - Can access data for any patient
>
> _System on behalf of a Patient_
> - Must have system-level scopes that match resource requested
> - Can only access data for the patient in itss context
>
> _System_
> - Must have system-level scopes that match resource requested
> - Can access data for any patient
>


- Invoke validation service
  - `POST https://(sandbox-)api.va.gov/internal/auth/v2/validation`
  - Headers:
    - `Authorization: Bearer ${TOKEN}`
    - `Host: ${VERIFICATION_HOST}` **_IS THIS NEEDED?_**
    - `apiKey: ${OAUTH_API_KEY}`
    - `Content-Type: application/x-www-form-urlencoded`
  - Body:
    - For supporting both Veteran and Clinical requests, i.e. _Data Query_
      - `aud: https://api.va.gov/services/fhir`
      - `aud: https://api.va.gov/services/clinical-fhir`
      - `strict: false`
    - For supporting just Clinical requests, i.e. _Vista FHIR Query_
      - `aud: https://api.va.gov/services/clinical-fhir`
      - `strict: true`
  - Check validation response
    - if `401`, return `401` with header
      - `WWW-Authenticate: Bearer`
    - if `429`, return `429`
    - if less than `200` or greater than `299`, return `500`
    - _Note: Token validation service will verify _User_ requests have access to patient data in Vista using a
      Charon API. Charon will verify that user has access to CPRS Vista menu option at the `launch.sta3n` using the
      user's DUZ determined from the `act.vista_id`_
  - Decode response (else `500`)
- Determine type of request from validation response (else `403`)
  - _Veteran_, e.g. through Apple Health  
    `act.icn == launch.patient`  
    Uses `patient/*` prefixed scopes
    `launch.patient` is required
  - _User_, e.g. CPM  
    `act.icn != null && act.icn != launch.patient`  
    `launch.patient` is optional  
    Uses `patient/*` prefixed scopes
  - _System_ to System, e.g. WellHive  
    `act.icn == null`  
    `launch.patient` is optional  
    If `launch.patient == null`, uses `system/*` prefixed scopes If `launch.patient != null`, uses `patient/*` prefixed
    scopes
- Verify scopes (else `403`)
  - Determine requested resource type from uri
  - Determine read/write operation of request
  - Verify `data.attributes.scp` contains one of the following scopes
    - `${prefix}/${resource}.${operation}`
    - `${prefix}/${resource}.*`
    - `${prefix}/*.${operation}`
    - `${prefix}/*.*`
- Determine if patient matching is required
  - Required if request type is _Veteran_
  - Required if request type is _User_ and `launch.patient != null`
  - Required if request type is _System_ and `launch.patient != null`
  - Not required if request type is _User_ and `launch.patient == null`
  - Not required if request type is _System_ and `launch.patient == null`
  - _Note: If request type is _User_ or _System_ and `launch.patient == null`, then requester will have access to
    all data._
- Verify pre-request patient matching (when required)
  - Determine requested ICN if possible
    - From patient read path (`base/Patient/${ICN}`)
    - From resource search query parameters (`base/${resource}?patient=${ICN}`)
    - Verify `launch.patient == ${requestedIcn}` (else `403`)
- Process request
  - Set response header comma-delimited list of unique ICNs for the records being returned or `NONE` if data does not
    include patient-centric data, e.g. a Location.
    - `X-VA-INCLUDES-ICNS: icn[,icn,icn,...] | NONE`
- Verify post-request patient matching (when required)
  - Verify response header `X-VA-INCLUDES-ICNS` is present (else `403`)
    - _Note: Service fulfilling the request must populate this header_
  - Verify `X-VA-INCLUDES-ICNS == NONE` or `X-VA-INCLUDES-ICNS == launch.patient` (else `403`)
    - If `X-VA-INCLUDES-ICNS` contains more than `launch.patient`, return `403`

### Static Access Token Validation

- Do not invoke token validation
- Do not check scopes
- Verify pre-request patient matching as describe above using pre-configured static access ICN
- Process request as described above
- Verify post-request patient matching as described above using pre-configured static access ICN

