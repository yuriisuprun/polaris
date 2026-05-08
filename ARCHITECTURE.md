# Free Tier Rate Limiting Architecture

## System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     API Request Received                         │
│              (POST /generate_flashcards, etc.)                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │  Check Cache       │
                    │  (SHA256 key)      │
                    └────────┬───────────┘
                             │
                    ┌────────▼────────┐
                    │ Cache Hit?      │
                    └────┬────────┬───┘
                         │        │
                    YES  │        │  NO
                         │        │
                    ┌────▼──┐  ┌─▼──────────────────┐
                    │Return │  │ Acquire Lock      │
                    │Cached │  │ (Serialize Req)   │
                    │Result │  └────┬───────────────┘
                    └───────┘       │
                                    ▼
                         ┌──────────────────────┐
                         │ Check Time Since     │
                         │ Last Request         │
                         └────┬────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │ Time < Min Interval?│
                    └────┬────────────┬──┘
                         │            │
                    YES  │            │  NO
                         │            │
                    ┌────▼──────┐  ┌─▼──────────────┐
                    │ Sleep     │  │ Update Last    │
                    │ Wait Time │  │ Request Time   │
                    └────┬──────┘  └─┬──────────────┘
                         │           │
                         └─────┬─────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ Make API Request     │
                    │ to OpenAI            │
                    └────┬────────────────┘
                         │
                    ┌────▼────────────────┐
                    │ Response Status?    │
                    └────┬────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    SUCCESS          429 ERROR      OTHER ERROR
        │                │                │
        ▼                ▼                ▼
    ┌────────┐    ┌──────────────┐  ┌──────────┐
    │ Cache  │    │ Check Error  │  │ Fall Back│
    │Result │    │ Type         │  │ to Mock  │
    │(5min) │    └──┬───────┬───┘  └──────────┘
    └───┬───┘       │       │
        │      QUOTA RATE LIMIT
        │      EXHAUSTED
        │           │
        │      ┌────▼──────────┐
        │      │ Attempt < Max?│
        │      └────┬────────┬─┘
        │           │        │
        │      YES  │        │  NO
        │           │        │
        │      ┌────▼──────┐ │
        │      │ Calculate │ │
        │      │ Backoff   │ │
        │      │ Delay     │ │
        │      │ 2^attempt │ │
        │      └────┬──────┘ │
        │           │        │
        │      ┌────▼──────┐ │
        │      │ Sleep     │ │
        │      │ Retry     │ │
        │      └────┬──────┘ │
        │           │        │
        │      ┌────▼──────┐ │
        │      │ Retry API │ │
        │      │ Request   │ │
        │      └────┬──────┘ │
        │           │        │
        │           └────┬───┘
        │                │
        │           ┌────▼──────────┐
        │           │ Fall Back to  │
        │           │ Mock Response │
        │           └────┬──────────┘
        │                │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────────┐
        │ Return Response    │
        │ (Real or Mock)     │
        └────────────────────┘
```

## Request Throttling Timeline

### Free Tier (3 requests per minute)

```
Time:    0s      20s     40s     60s     80s
         │       │       │       │       │
Request: ●───────●───────●───────●───────●
         │       │       │       │       │
         └─ 20s ─┘       └─ 20s ─┘       └─ 20s ─┘
         (throttle)      (throttle)      (throttle)

Each ● represents an API request
Each throttle period is 20 seconds
```

### Paid Tier (60 requests per minute)

```
Time:    0s  1s  2s  3s  4s  5s  6s  7s  8s  9s  10s
         │   │   │   │   │   │   │   │   │   │   │
Request: ●───●───●───●───●───●───●───●───●───●───●
         │   │   │   │   │   │   │   │   │   │   │
         └─1s─┘   └─1s─┘   └─1s─┘   └─1s─┘   └─1s─┘
         (throttle) (throttle) (throttle) (throttle)

Each ● represents an API request
Each throttle period is ~1 second
```

## Exponential Backoff Retry Timeline

### Rate Limit Scenario (429 Error)

```
Attempt 1: Make request → 429 Error
           │
           ├─ Wait 2 seconds
           │
Attempt 2: Make request → 429 Error
           │
           ├─ Wait 4 seconds
           │
Attempt 3: Make request → 429 Error
           │
           ├─ Wait 8 seconds
           │
Attempt 4: Make request → 429 Error
           │
           ├─ Wait 16 seconds
           │
Attempt 5: Make request → 429 Error
           │
           └─ Fall back to mock response

Total time: 2 + 4 + 8 + 16 = 30 seconds of waiting
```

## Cache Hit vs Miss Timeline

### Cache Miss (First Request)

```
Time:  0ms    100ms   500ms   1000ms  1500ms  2000ms
       │      │       │       │       │       │
       ├──────┤ Lock  ├───────┤ API   ├───────┤
       │      │       │       │ Call  │       │
       │      └───────┤ Check ├───────┤ Parse │
       │              │ Throttle      │ JSON  │
       │              │               │       │
       └──────────────────────────────┴───────┘
                    ~2000ms total
```

### Cache Hit (Subsequent Request)

```
Time:  0ms    10ms    20ms
       │      │       │
       ├──────┤ Check ├──────┤
       │      │ Cache │      │
       │      │       │      │
       └──────┴───────┴──────┘
            ~10ms total
```

## Error Handling Decision Tree

```
                    API Request
                         │
                         ▼
                    Response?
                    /    |    \
                   /     |     \
              SUCCESS  429   OTHER
                 │      │      │
                 │      ▼      │
                 │   Quota?    │
                 │   /    \    │
                 │  /      \   │
                 │ YES      NO │
                 │  │        │ │
                 │  │        │ │
                 │  ▼        ▼ ▼
                 │ MOCK   RETRY  MOCK
                 │  │      │      │
                 │  │      ▼      │
                 │  │   Attempt  │
                 │  │   < Max?   │
                 │  │   /    \   │
                 │  │  /      \  │
                 │  │ YES      NO│
                 │  │  │        │
                 │  │  ▼        ▼
                 │  │ WAIT    MOCK
                 │  │  │        │
                 │  │  ▼        │
                 │  │ RETRY     │
                 │  │  │        │
                 └──┴──┴────────┘
                      │
                      ▼
                  Return Response
```

## Component Interaction

```
┌─────────────────────────────────────────────────────────────┐
│                    FastAPI Router                            │
│              (app/tools/flashcards.py, etc.)                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│         (app/services/flashcards.py, etc.)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    LLM Client                                │
│              (app/core/llm.py)                              │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Request Throttling                                   │  │
│  │ - Global lock (serialize requests)                   │  │
│  │ - Min interval enforcement (20s for free tier)       │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Cache Layer                                          │  │
│  │ - SHA256 key (prompt + schema)                       │  │
│  │ - 5-minute TTL                                       │  │
│  │ - Max 128 items                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Retry Logic                                          │  │
│  │ - Exponential backoff (2, 4, 8, 16, 32s)             │  │
│  │ - Max 5 retries                                      │  │
│  │ - Quota detection                                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ OpenAI API Client                                    │  │
│  │ - Async HTTP requests                                │  │
│  │ - JSON schema validation                             │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Mock Fallback                                        │  │
│  │ - Deterministic responses                            │  │
│  │ - Same schema as real API                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Response                                  │
│              (Real or Mock)                                 │
└─────────────────────────────────────────────────────────────┘
```

## Configuration Impact

```
RATE_LIMIT_MIN_INTERVAL_S
│
├─ 20.0 (Free Tier)
│  └─ 3 requests per minute
│     └─ Suitable for: Learning, testing, low-volume usage
│
├─ 1.0 (Paid Tier - Starter)
│  └─ ~60 requests per minute
│     └─ Suitable for: Small applications, moderate usage
│
├─ 0.1 (Paid Tier - Professional)
│  └─ ~600 requests per minute
│     └─ Suitable for: Production applications, high volume
│
└─ 0.01 (Paid Tier - Enterprise)
   └─ ~6000 requests per minute
      └─ Suitable for: Large-scale applications

RATE_LIMIT_MAX_RETRIES
│
├─ 5 (Default)
│  └─ Total wait time: 2+4+8+16+32 = 62 seconds
│     └─ Suitable for: Free tier with occasional rate limits
│
├─ 3 (Paid Tier)
│  └─ Total wait time: 2+4+8 = 14 seconds
│     └─ Suitable for: Paid tier with better rate limits
│
└─ 1 (Aggressive)
   └─ Total wait time: 2 seconds
      └─ Suitable for: High-reliability requirements
```

## Performance Characteristics

```
Scenario                    Time        Notes
─────────────────────────────────────────────────────────
Cache Hit                   ~10ms       Instant from memory
First API Call              ~1-2s       Normal OpenAI latency
Rate Limited (1st retry)    ~2s + 1-2s  2s wait + API call
Rate Limited (5th retry)    ~32s + 1-2s 32s wait + API call
Mock Fallback               ~100ms      Deterministic response
Throttled Request           ~20s + 1-2s 20s wait + API call
```

## Monitoring Points

```
Request Flow
    │
    ├─ Throttle Check
    │  └─ Log: "Rate limit throttle: waiting X.Xs"
    │
    ├─ Cache Check
    │  └─ Log: "Cache hit for LLM request"
    │
    ├─ API Request
    │  └─ Log: "HTTP Request: POST https://api.openai.com/..."
    │
    ├─ Success
    │  └─ Log: "LLM request successful"
    │
    ├─ Rate Limited
    │  └─ Log: "OpenAI rate limited (429). Retrying in Xs"
    │
    ├─ Quota Exhausted
    │  └─ Log: "OpenAI quota exhausted, falling back to mock"
    │
    └─ Error
       └─ Log: "LLM client error: ..."
```
