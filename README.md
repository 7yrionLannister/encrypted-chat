# encrypted-chat

## How to use

### Locally

Client 1
```bash
java MainChat localhost 1234 localhost 1235
```

Client 2
```bash
java MainChat localhost 1235 localhost 1234
```

### Remote clients

Client 1
```bash
java MainChat hostA 1234 hostB 1235
```

Client 2
```bash
java MainChat hostB 1235 hostA 1234
```