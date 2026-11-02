package com.example.util

object SampleClashConfig {

    val SAMPLE_YAML = """
# Clash 示例配置文件 (含 VMess, VLESS, SS, Trojan, Hysteria2)
port: 7890
socks-port: 7891
allow-lan: false
mode: rule
log-level: info

proxies:
  - name: "🇭🇰 香港 01 - VMess 专线 [高速]"
    type: vmess
    server: 1.1.1.1
    port: 443
    uuid: a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
    alterId: 0
    cipher: auto
    tls: true
    servername: cloudflare.com
    network: ws
    ws-opts:
      path: /v2ray-stream
      headers:
        Host: hk01.speedstream.net

  - name: "🇯🇵 日本 02 - VLESS Reality [低延迟]"
    type: vless
    server: 1.0.0.1
    port: 443
    uuid: b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e
    network: tcp
    flow: xtls-rprx-vision
    tls: true
    servername: itunes.apple.com
    client-fingerprint: chrome
    reality-opts:
      public-key: jN3bO8Q6xK7zM5vP2rT4wY9aC1eG8hI0
      short-id: 6a8b0c2d

  - name: "🇺🇸 美国 01 - Shadowsocks 原生"
    type: ss
    server: 8.8.8.8
    port: 53
    cipher: aes-256-gcm
    password: SuperSecretPassWord2026

  - name: "🇸🇬 新加坡 01 - Trojan TLS"
    type: trojan
    server: 8.8.4.4
    port: 443
    password: mytrojansecurekey88
    sni: gateway.sg.speedtest.net
    skip-cert-verify: true

  - name: "🇩🇪 德国 01 - Hysteria2 极速"
    type: hysteria2
    server: 9.9.9.9
    port: 443
    auth: hy2-auth-token-fast
    sni: de01.hysteria.zone
    skip-cert-verify: false

  - name: "🇰🇷 韩国 01 - VMess gRPC [失效/离线节点]"
    type: vmess
    server: 198.51.100.254
    port: 443
    uuid: c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f
    alterId: 0
    cipher: auto
    tls: true
    network: grpc
    grpc-opts:
      grpc-service-name: kr-grpc-stream

  - name: "🇬🇧 英国 01 - SS 备用 [失效/离线节点]"
    type: ss
    server: 203.0.113.199
    port: 8848
    cipher: chacha20-ietf-poly1305
    password: expiredpassword123

  - name: "🇹🇼 台湾 01 - VLESS WebSocket"
    type: vless
    server: 104.16.132.229
    port: 443
    uuid: d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f90
    network: ws
    tls: true
    servername: tw01.accelerate.io
    ws-opts:
      path: /tw-fast
      headers:
        Host: tw01.accelerate.io
""".trimIndent()

    val SAMPLE_MINIMAL = """
proxies:
  - name: "HK-VMess"
    type: vmess
    server: 1.1.1.1
    port: 443
    uuid: 00000000-0000-0000-0000-000000000001
    alterId: 0
    cipher: auto
    tls: true
    network: ws
    ws-opts:
      path: /ws
  - name: "US-SS"
    type: ss
    server: 8.8.8.8
    port: 53
    cipher: aes-128-gcm
    password: password123
""".trimIndent()
}
