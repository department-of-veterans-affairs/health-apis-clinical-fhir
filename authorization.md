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
- Invoke validation service
    - `POST https://(sandbox-)api.va.gov/internal/auth/v1/validation`
    - Headers:
        - `Authorization: Bearer ${TOKEN}`
        - `Host: ${VERIFICATION_HOST}` **_IS THIS NEEDED?_**
        - `apiKey: ${OAUTH_API_KEY}`
        - `Content-Type: application/x-www-form-urlencoded`
    - Body:
        - `aud: https://api.va.gov/services/fhir`
    - Check validation response
        - if `401`, return `401` with header
            - `WWW-Authenticate: Bearer`
        - if `429`, return `429`
        - if less than `200` or greater than `299`, return `500`
    - Decode response (else `500`)
- Determine type of request from validation response (else `403`)
    - _Veteran_, e.g. through Apple Health  
      `act.icn == launch.patient`  
      Uses `patient/*` prefixed scopes
      `launch.patient` is required
    - _Clinician_, e.g. CPM  
      `act.icn != null && act.icn != launch.patient`  
      `launch.patient` is optional  
      Uses `patient/*` prefixed scopes
    - _System_ to System, e.g. WellHive  
      `act.icn == null`  
      `launch.patient` is optional  
      Uses `system/*` prefixed scopes
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
    - Required if request type is _Clinician_ and `launch.patient != null`  
    - Not required if request type is _Clinician_ and `launch.patient == null`  
    - Not required if request type is _System_
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
  - Verify `X-VA-INCLUDES-ICNS == NONE` or `X-VA-INCLUDES-ICNS == launch.patient` (else `403`)
    - If `X-VA-INCLUDES-ICNS` contains more than `launch.patient`, return `403` 
    

### Static Access Token Validation
- Do not invoke token validation
- Do not check scopes  
- Verify pre-request patient matching as describe above using pre-configured static access ICN
- Process request as described above
- Verify post-request patient matching as described above using pre-configured static access ICN

