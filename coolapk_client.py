#!/usr/bin/env python3
"""
酷安 Token API 客户端

使用方法:
    from coolapk_client import CoolapkClient
    
    client = CoolapkClient("http://localhost:8080")
    token = client.get_token(device_id)
    data = client.request_api("/v6/main/indexV8")
"""

import requests
import time
from typing import Optional, Dict, Any


class CoolapkClient:
    """酷安 API 客户端"""
    
    DEFAULT_DEVICE_ID = "sxWduByOxADMuITM5ADNy4SQzEVQgszREZjTQZENwMjMgsTat9WYphFI7kWbvFWaYByOgsDI7AyOwc2d3gXY1pVMvNFSsZTR5pUZE5mM2oWQvpnc3IkSWh0aEVFR"
    VERSION_CODE = "2512091"
    VERSION_NAME = "15.9.1"
    PACKAGE_NAME = "com.coolapk.market"
    
    def __init__(self, token_api_url: str = "http://localhost:8080", device_id: str = None):
        self.token_api_url = token_api_url.rstrip("/")
        self.device_id = device_id or self.DEFAULT_DEVICE_ID
        self.session = requests.Session()
    
    def get_token(self, device_id: str = None) -> str:
        """从 API 获取 token"""
        device_id = device_id or self.device_id
        
        resp = self.session.get(
            f"{self.token_api_url}/token",
            params={"device_id": device_id},
            timeout=30
        )
        resp.raise_for_status()
        return resp.json()["token"]
    
    def health_check(self) -> bool:
        """检查 API 服务是否正常"""
        try:
            resp = self.session.get(f"{self.token_api_url}/health", timeout=5)
            return resp.status_code == 200
        except:
            return False
    
    def get_headers(self, token: str = None) -> Dict[str, str]:
        """获取请求头"""
        if token is None:
            token = self.get_token()
        
        return {
            "User-Agent": f"Dalvik/2.1.0 (Linux; U; Android 15; Pixel 8 Build/AP2A.240805.005) (#Build; google; Pixel 8; AP2A.240805.005; 15) +CoolMarket/{self.VERSION_NAME}-{self.VERSION_CODE}-universal",
            "X-Requested-With": "XMLHttpRequest",
            "X-Sdk-Int": "35",
            "X-Sdk-Locale": "zh-CN",
            "X-App-Id": self.PACKAGE_NAME,
            "X-App-Token": token,
            "X-App-Version": self.VERSION_NAME,
            "X-App-Code": self.VERSION_CODE,
            "X-Api-Version": "15",
            "X-App-Device": self.device_id,
            "X-Dark-Mode": "0",
            "X-App-Channel": "coolapk",
            "X-App-Mode": "universal",
            "X-App-Supported": self.VERSION_CODE,
            "Accept-Encoding": "gzip",
        }
    
    def request_api(
        self,
        endpoint: str,
        method: str = "GET",
        params: Dict[str, Any] = None,
        data: Dict[str, Any] = None,
        token: str = None
    ) -> Dict[str, Any]:
        """请求酷安 API"""
        url = f"https://api2.coolapk.com{endpoint}"
        headers = self.get_headers(token)
        
        resp = self.session.request(
            method=method,
            url=url,
            params=params,
            data=data,
            headers=headers,
            timeout=10
        )
        resp.raise_for_status()
        return resp.json()
    
    def get_index(self, page: int = 1) -> Dict[str, Any]:
        """获取首页数据"""
        return self.request_api(
            "/v6/main/indexV8",
            params={
                "page": page,
                "firstLaunch": 0,
                "installTime": str(int(time.time() * 1000)),
                "ids": ""
            }
        )
    
    def get_feed(self, feed_id: int) -> Dict[str, Any]:
        """获取动态详情"""
        return self.request_api(f"/v6/feed/detail", params={"id": feed_id})
    
    def search(self, keyword: str, page: int = 1) -> Dict[str, Any]:
        """搜索"""
        return self.request_api(
            "/v6/search",
            params={
                "type": "all",
                "feedType": "all",
                "sort": "default",
                "searchValue": keyword,
                "page": page
            }
        )


def main():
    """测试客户端"""
    print("=" * 60)
    print("酷安 API 客户端测试")
    print("=" * 60)
    
    client = CoolapkClient()
    
    # 健康检查
    print("\n[*] Checking API health...")
    if not client.health_check():
        print("[-] API service not available")
        return
    print("[+] API service is healthy")
    
    # 获取 token
    print("\n[*] Getting token...")
    token = client.get_token()
    print(f"[+] Token: {token[:50]}...")
    
    # 获取首页
    print("\n[*] Fetching index...")
    try:
        data = client.get_index()
        if data.get("status") == 0 or "data" in data:
            print(f"[+] Success! Got {len(data.get('data', []))} items")
        else:
            print(f"[-] Error: {data.get('message', 'Unknown')}")
    except Exception as e:
        print(f"[-] Request failed: {e}")


if __name__ == "__main__":
    main()
