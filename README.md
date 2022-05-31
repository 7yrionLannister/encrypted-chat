# encrypted-chat

## How to use

### Locally

#### Client 1
```bash
java MainChat localhost 1234 localhost 1235
```

#### Client 2
```bash
java MainChat localhost 1235 localhost 1234
```

### Remote clients

#### Client 1
```bash
java MainChat hostA 1234 hostB 1235
```

#### Client 2
```bash
java MainChat hostB 1235 hostA 1234
```
## How was the chat made?

1. We used [this](https://www.youtube.com/watch?v=gLfuZrrfKes&t=896s) project as a starting point. There is no encryption though, so everything going through the network is in plain text.
2. As the application is supposed to be point to point, we removed the server and the logic to manage multiple clients. We focused on having a connection that allowed two clients to exchange messages in plain text.
3. When we accomplished the above objective, we introduced the Diffie-Hellman algorithm to exchange a secret key.
4. Finally, we used the key to encrypt and decrypt all the messages of the communication using AES-128.

The program was tested and is known to work with two computers in the same network.

## Conclusions

It is relatively easy to create a secure chat using the built-in encryption libraries in the JDK. The security of the application depends on the size of the prime number P.
