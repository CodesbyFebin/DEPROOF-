# Requirement priorities (281 unresolved)

Generated from `evidence/qualification/prioritized-backlog.json` by the classification below. Not a status re-derivation.

| Primary gap | Count | Note |
|---|---:|---|
| missing_code | 27 | No implementation path, or MISSING_IMPLEMENTATION |
| missing_local_tests | 199 | Implementation present, no tests listed; locally actionable |
| local_verification_remaining | 43 | Tests listed but not closed; locally actionable |
| device_validation | 7 | Needs physical device or Android Keystore evidence |
| external_integration | 5 | Needs authorized external protocol/provider access |

Locally actionable without device or external access: 269. Of these, 96 also need a device later.

## Ordered list

| # | ID | P | Phase | Bucket | Device also | External also |
|---:|---|---:|---|---|:-:|:-:|
| 1 | C047 | 0 | P1 | missing_local_tests | yes |  |
| 2 | C048 | 0 | P1 | missing_local_tests | yes |  |
| 3 | C095 | 0 | P1 | missing_local_tests | yes |  |
| 4 | F040 | 0 | P1 | missing_local_tests | yes |  |
| 5 | F041 | 0 | P1 | missing_local_tests | yes |  |
| 6 | F045 | 0 | P1 | missing_local_tests | yes |  |
| 7 | F046 | 0 | P1 | missing_local_tests | yes |  |
| 8 | F047 | 0 | P1 | missing_local_tests | yes |  |
| 9 | F048 | 0 | P1 | missing_local_tests | yes |  |
| 10 | F049 | 0 | P1 | missing_local_tests | yes |  |
| 11 | FN062 | 0 | P1 | missing_local_tests | yes |  |
| 12 | F067 | 0 | P1 | local_verification_remaining | yes |  |
| 13 | F079 | 0 | P1 | local_verification_remaining | yes |  |
| 14 | FN048 | 0 | P1 | local_verification_remaining | yes |  |
| 15 | FN055 | 0 | P1 | local_verification_remaining | yes |  |
| 16 | F060 | 0 | P2 | local_verification_remaining | yes |  |
| 17 | FN051 | 0 | P2 | local_verification_remaining | yes |  |
| 18 | FN052 | 0 | P2 | local_verification_remaining | yes |  |
| 19 | F118 | 0 | P5 | local_verification_remaining | yes |  |
| 20 | F097 | 0 | P1 | device_validation | yes |  |
| 21 | F117 | 1 | P4 | local_verification_remaining |  |  |
| 22 | F058 | 1 | P2 | device_validation | yes |  |
| 23 | F105 | 1 | P2 | device_validation | yes |  |
| 24 | F115 | 1 | P2 | device_validation | yes |  |
| 25 | FN042 | 2 | P1 | missing_code |  |  |
| 26 | EF009 | 2 | P3 | missing_code |  |  |
| 27 | EF017 | 2 | P3 | missing_code |  |  |
| 28 | EF019 | 2 | P3 | missing_code |  |  |
| 29 | FN056 | 2 | P3 | missing_code |  |  |
| 30 | FN057 | 2 | P3 | missing_code |  |  |
| 31 | FN058 | 2 | P3 | missing_code |  |  |
| 32 | FN059 | 2 | P3 | missing_code |  |  |
| 33 | E001 | 2 | P1 | missing_local_tests |  |  |
| 34 | E003 | 2 | P1 | missing_local_tests |  |  |
| 35 | E004 | 2 | P1 | missing_local_tests |  |  |
| 36 | E005 | 2 | P1 | missing_local_tests |  |  |
| 37 | E006 | 2 | P1 | missing_local_tests |  |  |
| 38 | E007 | 2 | P1 | missing_local_tests |  |  |
| 39 | E026 | 2 | P1 | missing_local_tests |  |  |
| 40 | E027 | 2 | P1 | missing_local_tests |  |  |
| 41 | E028 | 2 | P1/P2 | missing_local_tests |  |  |
| 42 | E029 | 2 | P1/P4 | missing_local_tests |  |  |
| 43 | F001 | 2 | P1 | missing_local_tests |  |  |
| 44 | F005 | 2 | P1 | missing_local_tests |  |  |
| 45 | F006 | 2 | P1 | missing_local_tests |  |  |
| 46 | F007 | 2 | P1 | missing_local_tests |  |  |
| 47 | F009 | 2 | P1 | missing_local_tests |  |  |
| 48 | F010 | 2 | P1 | missing_local_tests |  |  |
| 49 | F011 | 2 | P1 | missing_local_tests |  |  |
| 50 | F012 | 2 | P1 | missing_local_tests |  |  |
| 51 | F013 | 2 | P1 | missing_local_tests |  |  |
| 52 | F014 | 2 | P1 | missing_local_tests |  |  |
| 53 | F015 | 2 | P1 | missing_local_tests |  |  |
| 54 | F016 | 2 | P1 | missing_local_tests |  |  |
| 55 | F017 | 2 | P1 | missing_local_tests |  |  |
| 56 | F018 | 2 | P1 | missing_local_tests |  |  |
| 57 | F019 | 2 | P1 | missing_local_tests |  |  |
| 58 | F020 | 2 | P1 | missing_local_tests |  |  |
| 59 | F021 | 2 | P1 | missing_local_tests |  |  |
| 60 | F022 | 2 | P1 | missing_local_tests |  |  |
| 61 | F023 | 2 | P1 | missing_local_tests |  |  |
| 62 | F024 | 2 | P1 | missing_local_tests |  |  |
| 63 | F025 | 2 | P1 | missing_local_tests |  |  |
| 64 | F026 | 2 | P1 | missing_local_tests |  |  |
| 65 | F027 | 2 | P1 | missing_local_tests |  |  |
| 66 | F028 | 2 | P1 | missing_local_tests |  |  |
| 67 | F029 | 2 | P1 | missing_local_tests |  |  |
| 68 | F030 | 2 | P1 | missing_local_tests |  |  |
| 69 | F031 | 2 | P1 | missing_local_tests |  |  |
| 70 | F032 | 2 | P1 | missing_local_tests |  |  |
| 71 | F034 | 2 | P1 | missing_local_tests |  |  |
| 72 | F035 | 2 | P1 | missing_local_tests |  |  |
| 73 | F036 | 2 | P1 | missing_local_tests |  |  |
| 74 | F037 | 2 | P1 | missing_local_tests |  |  |
| 75 | F038 | 2 | P1 | missing_local_tests |  |  |
| 76 | F039 | 2 | P1 | missing_local_tests |  |  |
| 77 | F042 | 2 | P1 | missing_local_tests |  |  |
| 78 | F043 | 2 | P1 | missing_local_tests |  |  |
| 79 | F044 | 2 | P1 | missing_local_tests |  |  |
| 80 | F050 | 2 | P1 | missing_local_tests |  |  |
| 81 | F064 | 2 | P1 | missing_local_tests |  |  |
| 82 | F066 | 2 | P1 | missing_local_tests |  |  |
| 83 | F068 | 2 | P1 | missing_local_tests |  |  |
| 84 | F069 | 2 | P1 | missing_local_tests |  |  |
| 85 | F070 | 2 | P1 | missing_local_tests |  |  |
| 86 | F075 | 2 | P1 | missing_local_tests |  |  |
| 87 | F076 | 2 | P1 | missing_local_tests |  |  |
| 88 | F077 | 2 | P1 | missing_local_tests |  |  |
| 89 | F078 | 2 | P1 | missing_local_tests |  |  |
| 90 | F080 | 2 | P1 | missing_local_tests |  |  |
| 91 | F091 | 2 | P1 | missing_local_tests |  |  |
| 92 | F092 | 2 | P1 | missing_local_tests |  |  |
| 93 | F094 | 2 | P1 | missing_local_tests |  |  |
| 94 | F096 | 2 | P1 | missing_local_tests |  |  |
| 95 | F098 | 2 | P1 | missing_local_tests |  |  |
| 96 | F099 | 2 | P1 | missing_local_tests |  |  |
| 97 | F100 | 2 | P1 | missing_local_tests |  |  |
| 98 | FN005 | 2 | P1 | missing_local_tests |  |  |
| 99 | FN006 | 2 | P1 | missing_local_tests |  |  |
| 100 | FN007 | 2 | P1 | missing_local_tests |  |  |
| 101 | FN008 | 2 | P1 | missing_local_tests |  |  |
| 102 | FN017 | 2 | P1 | missing_local_tests |  |  |
| 103 | FN018 | 2 | P1 | missing_local_tests |  |  |
| 104 | FN019 | 2 | P1 | missing_local_tests |  |  |
| 105 | FN022 | 2 | P1 | missing_local_tests |  |  |
| 106 | FN023 | 2 | P1 | missing_local_tests |  |  |
| 107 | FN024 | 2 | P1 | missing_local_tests |  |  |
| 108 | FN025 | 2 | P1 | missing_local_tests |  |  |
| 109 | FN026 | 2 | P1 | missing_local_tests |  |  |
| 110 | FN027 | 2 | P1 | missing_local_tests |  |  |
| 111 | FN028 | 2 | P1 | missing_local_tests |  |  |
| 112 | FN029 | 2 | P1 | missing_local_tests |  |  |
| 113 | FN030 | 2 | P1 | missing_local_tests |  |  |
| 114 | FN035 | 2 | P1 | missing_local_tests |  |  |
| 115 | FN036 | 2 | P1 | missing_local_tests |  |  |
| 116 | FN037 | 2 | P1 | missing_local_tests |  |  |
| 117 | FN038 | 2 | P1 | missing_local_tests |  |  |
| 118 | FN040 | 2 | P1 | missing_local_tests |  |  |
| 119 | FN041 | 2 | P1 | missing_local_tests |  |  |
| 120 | FN043 | 2 | P1 | missing_local_tests |  |  |
| 121 | FN044 | 2 | P1 | missing_local_tests |  |  |
| 122 | FN045 | 2 | P1 | missing_local_tests |  |  |
| 123 | FN046 | 2 | P1 | missing_local_tests |  |  |
| 124 | FN047 | 2 | P1 | missing_local_tests |  |  |
| 125 | FN049 | 2 | P1 | missing_local_tests |  |  |
| 126 | FN054 | 2 | P1 | missing_local_tests |  |  |
| 127 | F051 | 2 | P2 | missing_local_tests |  |  |
| 128 | F052 | 2 | P2 | missing_local_tests |  |  |
| 129 | F053 | 2 | P2 | missing_local_tests |  |  |
| 130 | F054 | 2 | P2 | missing_local_tests |  |  |
| 131 | F056 | 2 | P2 | missing_local_tests |  |  |
| 132 | F057 | 2 | P2 | missing_local_tests |  |  |
| 133 | F059 | 2 | P2 | missing_local_tests |  |  |
| 134 | F071 | 2 | P2 | missing_local_tests |  |  |
| 135 | F072 | 2 | P2 | missing_local_tests |  |  |
| 136 | F073 | 2 | P2 | missing_local_tests |  |  |
| 137 | F074 | 2 | P2 | missing_local_tests |  |  |
| 138 | E020 | 2 | P3 | missing_local_tests |  |  |
| 139 | E022 | 2 | P3 | missing_local_tests |  |  |
| 140 | EF024 | 2 | P3 | missing_local_tests |  |  |
| 141 | F109 | 2 | P3 | missing_local_tests |  |  |
| 142 | EF023 | 2 | P4 | missing_local_tests |  |  |
| 143 | F120 | 2 | P4 | missing_local_tests |  |  |
| 144 | FN060 | 2 | P4 | missing_local_tests |  |  |
| 145 | F114 | 2 | P5 | missing_local_tests |  |  |
| 146 | E002 | 2 | P1 | local_verification_remaining |  |  |
| 147 | F003 | 2 | P1 | local_verification_remaining |  |  |
| 148 | F008 | 2 | P1 | local_verification_remaining |  |  |
| 149 | F093 | 2 | P1 | local_verification_remaining |  |  |
| 150 | FN004 | 2 | P1 | local_verification_remaining |  |  |
| 151 | E011 | 2 | P3 | local_verification_remaining |  |  |
| 152 | E012 | 2 | P3 | local_verification_remaining |  |  |
| 153 | E013 | 2 | P3 | local_verification_remaining |  |  |
| 154 | E014 | 2 | P3 | local_verification_remaining |  |  |
| 155 | E016 | 2 | P3 | local_verification_remaining |  |  |
| 156 | E017 | 2 | P3 | local_verification_remaining |  |  |
| 157 | E018 | 2 | P3 | local_verification_remaining |  |  |
| 158 | E019 | 2 | P3 | local_verification_remaining |  |  |
| 159 | E021 | 2 | P3 | local_verification_remaining |  |  |
| 160 | E023 | 2 | P3 | local_verification_remaining |  |  |
| 161 | E024 | 2 | P3 | local_verification_remaining |  |  |
| 162 | E025 | 2 | P3 | local_verification_remaining |  |  |
| 163 | EF001 | 2 | P3 | local_verification_remaining |  |  |
| 164 | EF004 | 2 | P3 | local_verification_remaining |  |  |
| 165 | EF005 | 2 | P3 | local_verification_remaining |  |  |
| 166 | EF008 | 2 | P3 | local_verification_remaining |  |  |
| 167 | EF011 | 2 | P3 | local_verification_remaining |  |  |
| 168 | EF012 | 2 | P3 | local_verification_remaining |  |  |
| 169 | EF013 | 2 | P3 | local_verification_remaining |  |  |
| 170 | EF016 | 2 | P3 | local_verification_remaining |  |  |
| 171 | EF018 | 2 | P3 | local_verification_remaining |  |  |
| 172 | EF020 | 2 | P3 | local_verification_remaining |  |  |
| 173 | F104 | 2 | P3 | local_verification_remaining |  |  |
| 174 | F108 | 2 | P3 | local_verification_remaining |  |  |
| 175 | F119 | 2 | P4 | local_verification_remaining |  |  |
| 176 | F116 | 2 | P5 | local_verification_remaining |  |  |
| 177 | E030 | 2 | All phases | local_verification_remaining |  |  |
| 178 | F081 | 3 | P3 | missing_code |  | yes |
| 179 | F082 | 3 | P3 | missing_code |  | yes |
| 180 | F083 | 3 | P3 | missing_code |  | yes |
| 181 | F084 | 3 | P3 | missing_code |  | yes |
| 182 | F085 | 3 | P3 | missing_code |  | yes |
| 183 | F086 | 3 | P3 | missing_code |  | yes |
| 184 | F087 | 3 | P3 | missing_code |  | yes |
| 185 | F088 | 3 | P3 | missing_code |  | yes |
| 186 | F089 | 3 | P3 | missing_code |  | yes |
| 187 | F090 | 3 | P3 | missing_code |  | yes |
| 188 | F101 | 3 | P3 | missing_code |  | yes |
| 189 | F102 | 3 | P3 | missing_code |  | yes |
| 190 | F103 | 3 | P3 | missing_code |  | yes |
| 191 | F106 | 3 | P3 | missing_code |  | yes |
| 192 | F107 | 3 | P3 | missing_code |  | yes |
| 193 | F110 | 3 | P3 | missing_code |  | yes |
| 194 | F111 | 3 | P5 | missing_code |  | yes |
| 195 | F112 | 3 | P5 | missing_code |  | yes |
| 196 | F113 | 3 | P5 | missing_code |  | yes |
| 197 | C001 | 3 | P1 | missing_local_tests | yes |  |
| 198 | C005 | 3 | P1 | missing_local_tests | yes |  |
| 199 | C006 | 3 | P1 | missing_local_tests | yes |  |
| 200 | C007 | 3 | P1 | missing_local_tests | yes |  |
| 201 | C009 | 3 | P1 | missing_local_tests | yes |  |
| 202 | C010 | 3 | P1 | missing_local_tests | yes |  |
| 203 | C011 | 3 | P1 | missing_local_tests | yes |  |
| 204 | C012 | 3 | P1 | missing_local_tests | yes |  |
| 205 | C013 | 3 | P1 | missing_local_tests | yes |  |
| 206 | C014 | 3 | P1 | missing_local_tests | yes |  |
| 207 | C015 | 3 | P1 | missing_local_tests | yes |  |
| 208 | C016 | 3 | P1 | missing_local_tests | yes |  |
| 209 | C017 | 3 | P1 | missing_local_tests | yes |  |
| 210 | C018 | 3 | P1 | missing_local_tests | yes |  |
| 211 | C019 | 3 | P1 | missing_local_tests | yes |  |
| 212 | C020 | 3 | P1 | missing_local_tests | yes |  |
| 213 | C021 | 3 | P1 | missing_local_tests | yes |  |
| 214 | C022 | 3 | P1 | missing_local_tests | yes |  |
| 215 | C023 | 3 | P1 | missing_local_tests | yes |  |
| 216 | C024 | 3 | P1 | missing_local_tests | yes |  |
| 217 | C025 | 3 | P1 | missing_local_tests | yes |  |
| 218 | C026 | 3 | P1 | missing_local_tests | yes |  |
| 219 | C028 | 3 | P1 | missing_local_tests | yes |  |
| 220 | C029 | 3 | P1 | missing_local_tests | yes |  |
| 221 | C030 | 3 | P1 | missing_local_tests | yes |  |
| 222 | C034 | 3 | P1 | missing_local_tests | yes |  |
| 223 | C038 | 3 | P1 | missing_local_tests | yes |  |
| 224 | C039 | 3 | P1 | missing_local_tests | yes |  |
| 225 | C040 | 3 | P1 | missing_local_tests | yes |  |
| 226 | C044 | 3 | P1 | missing_local_tests | yes |  |
| 227 | C050 | 3 | P1 | missing_local_tests | yes |  |
| 228 | C051 | 3 | P1 | missing_local_tests | yes |  |
| 229 | C052 | 3 | P1 | missing_local_tests | yes |  |
| 230 | C053 | 3 | P1 | missing_local_tests | yes |  |
| 231 | C054 | 3 | P1 | missing_local_tests | yes |  |
| 232 | C055 | 3 | P1 | missing_local_tests | yes |  |
| 233 | C056 | 3 | P1 | missing_local_tests | yes |  |
| 234 | C057 | 3 | P1 | missing_local_tests | yes |  |
| 235 | C058 | 3 | P1 | missing_local_tests | yes |  |
| 236 | C059 | 3 | P1 | missing_local_tests | yes |  |
| 237 | C060 | 3 | P1 | missing_local_tests | yes |  |
| 238 | C061 | 3 | P1 | missing_local_tests | yes |  |
| 239 | C062 | 3 | P1 | missing_local_tests | yes |  |
| 240 | C063 | 3 | P1 | missing_local_tests | yes |  |
| 241 | C064 | 3 | P1 | missing_local_tests | yes |  |
| 242 | C065 | 3 | P1 | missing_local_tests | yes |  |
| 243 | C066 | 3 | P1 | missing_local_tests | yes |  |
| 244 | C067 | 3 | P1 | missing_local_tests | yes |  |
| 245 | C068 | 3 | P1 | missing_local_tests | yes |  |
| 246 | C069 | 3 | P1 | missing_local_tests | yes |  |
| 247 | C071 | 3 | P1 | missing_local_tests | yes |  |
| 248 | C072 | 3 | P1 | missing_local_tests | yes |  |
| 249 | C074 | 3 | P1 | missing_local_tests | yes |  |
| 250 | C075 | 3 | P1 | missing_local_tests | yes |  |
| 251 | C076 | 3 | P1 | missing_local_tests | yes |  |
| 252 | C077 | 3 | P1 | missing_local_tests | yes |  |
| 253 | C078 | 3 | P1 | missing_local_tests | yes |  |
| 254 | C079 | 3 | P1 | missing_local_tests | yes |  |
| 255 | C080 | 3 | P1 | missing_local_tests | yes |  |
| 256 | C081 | 3 | P1 | missing_local_tests | yes |  |
| 257 | C082 | 3 | P1 | missing_local_tests | yes |  |
| 258 | C083 | 3 | P1 | missing_local_tests | yes |  |
| 259 | C084 | 3 | P1 | missing_local_tests | yes |  |
| 260 | C085 | 3 | P1 | missing_local_tests | yes |  |
| 261 | C086 | 3 | P1 | missing_local_tests | yes |  |
| 262 | C088 | 3 | P1 | missing_local_tests | yes |  |
| 263 | C089 | 3 | P1 | missing_local_tests | yes |  |
| 264 | C090 | 3 | P1 | missing_local_tests | yes |  |
| 265 | C091 | 3 | P1 | missing_local_tests | yes |  |
| 266 | C092 | 3 | P1 | missing_local_tests | yes |  |
| 267 | C093 | 3 | P1 | missing_local_tests | yes |  |
| 268 | C094 | 3 | P1 | missing_local_tests | yes |  |
| 269 | C096 | 3 | P1 | missing_local_tests | yes |  |
| 270 | C098 | 3 | P1 | missing_local_tests | yes |  |
| 271 | C099 | 3 | P1 | missing_local_tests | yes |  |
| 272 | C003 | 3 | P1 | local_verification_remaining | yes |  |
| 273 | C008 | 3 | P1 | local_verification_remaining | yes |  |
| 274 | F095 | 3 | P1 | device_validation | yes |  |
| 275 | FN033 | 3 | P1 | device_validation | yes |  |
| 276 | F055 | 3 | P2 | device_validation | yes |  |
| 277 | E008 | 3 | P1/P3 | external_integration |  | yes |
| 278 | FN061 | 3 | P1 | external_integration |  | yes |
| 279 | E009 | 3 | P3 | external_integration |  | yes |
| 280 | E010 | 3 | P3 | external_integration |  | yes |
| 281 | E015 | 3 | P3 | external_integration |  | yes |

## Local test evidence (review binding, first wave)

- C048: app/src/test/java/com/example/domain/ReviewBindingTest.kt::signingGateRefusesMessageChangedAfterReview; app/src/test/java/com/example/domain/CoreTest.kt::completeMessageMutationsNeverRetainApproval
- F045: app/src/test/java/com/example/domain/ReviewBindingTest.kt::storedHashesAreUnaffectedByCallerBufferChanges; app/src/test/java/com/example/domain/ReviewBindingTest.kt::messageHashIsIndependentOfReviewContextButContextHashIsNot
- F046: app/src/test/java/com/example/domain/ReviewBindingTest.kt::reviewTimestampChangeIsRejected; app/src/test/java/com/example/domain/ReviewBindingTest.kt::feeAndBlockHeightChangesAreRejected; app/src/test/java/com/example/domain/ReviewBindingTest.kt::signingGateRefusesWalletAccountThatIsNotTheFeePayer
- F047: app/src/test/java/com/example/domain/ReviewBindingTest.kt::signingGateRefusesMessageChangedAfterReview
- F048: app/src/test/java/com/example/domain/ReviewBindingTest.kt::reviewTimestampChangeIsRejected; app/src/test/java/com/example/domain/CoreTest.kt::memoParserAndImmutableMessageContextMutations

Registry statuses are unchanged. A local unit test is evidence of the logic only, not of device acceptance.
