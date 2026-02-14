import firebase_admin
from firebase_admin import credentials, messaging
import logging
import os
import json
from typing import List

logger = logging.getLogger(__name__)

class FCMService:
    def __init__(self):
        # Initialize Firebase Admin SDK
        # In production, use environment variable or a secure file path
        # For Render/Railway, we can pass the JSON content via ENV var
        if not firebase_admin._apps:
            try:
                # Option 1: Path to serviceAccountKey.json (Local dev)
                cred_path = "serviceAccountKey.json"
                
                # Option 2: Environment Variable Content (Production)
                firebase_creds_json = os.environ.get("FIREBASE_CREDENTIALS")
                
                if firebase_creds_json:
                    cred_dict = json.loads(firebase_creds_json)
                    cred = credentials.Certificate(cred_dict)
                    firebase_admin.initialize_app(cred)
                    logger.info("Firebase Admin initialized via Environment Variable")
                elif os.path.exists(cred_path):
                    cred = credentials.Certificate(cred_path)
                    firebase_admin.initialize_app(cred)
                    logger.info("Firebase Admin initialized via serviceAccountKey.json")
                else:
                    logger.warning("Firebase Credentials not found. FCM will not work.")
            except Exception as e:
                logger.error(f"Failed to initialize Firebase Admin: {e}")

    def send_to_topic(self, topic: str, title: str, body: str):
        try:
            if not firebase_admin._apps:
                logger.warning("Firebase not initialized. Skipping notification.")
                return

            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                topic=topic,
            )
            response = messaging.send(message)
            logger.info(f"Successfully sent message to topic {topic}: {response}")
        except Exception as e:
            logger.error(f"Error sending message to topic {topic}: {e}")

    def send_multicast(self, tokens: List[str], title: str, body: str, data: dict = None):
        """Send message to specific devices (e.g. users watching a market)"""
        try:
            if not firebase_admin._apps or not tokens:
                return

            # Batch send (up to 500 tokens per batch)
            message = messaging.MulticastMessage(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=data,
                tokens=tokens,
            )
            response = messaging.send_multicast(message)
            logger.info(f"Sent multicast message: {response.success_count} successes, {response.failure_count} failures")
        except Exception as e:
            logger.error(f"Error sending multicast message: {e}")
