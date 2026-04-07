import * as functions from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

const payHeroUsername = defineSecret("PAYHERO_USERNAME");
const payHeroPassword = defineSecret("PAYHERO_PASSWORD");

const PAYHERO_API = "https://backend.payhero.co.ke/api/v2/payments";

async function callPayHeroSTK(params: {
  amount: number;
  phoneNumber: string;
  channelId: number;
  externalReference: string;
  customerName: string;
  callbackUrl: string;
  username: string;
  password: string;
}): Promise<{ reference: string; checkoutRequestId: string }> {
  const auth = Buffer.from(`${params.username}:${params.password}`).toString("base64");

  const response = await fetch(PAYHERO_API, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Basic ${auth}`,
    },
    body: JSON.stringify({
      amount: params.amount,
      phone_number: params.phoneNumber,
      channel_id: params.channelId,
      provider: "m-pesa",
      external_reference: params.externalReference,
      customer_name: params.customerName,
      callback_url: params.callbackUrl,
    }),
  });

  if (!response.ok) {
    const err = await response.text();
    throw new Error(`PayHero API error ${response.status}: ${err}`);
  }

  const data = await response.json() as {
    success: boolean;
    reference: string;
    CheckoutRequestID: string;
  };

  if (!data.success) {
    throw new Error(`PayHero rejected: ${JSON.stringify(data)}`);
  }

  return { reference: data.reference, checkoutRequestId: data.CheckoutRequestID };
}

async function getPropertyConfig() {
  const doc = await db.collection("config").doc("property").get();
  return doc.data() as {
    rentPaybill: string;
    rentAccountPrefix: string;
    internetTillNumber: string;
    payHeroChannelId: number;
    kplcPaybill: string;
  };
}

export const initiateRentPayment = functions.onCall(
  { secrets: [payHeroUsername, payHeroPassword] },
  async (request) => {
    const { tenantId, unitId, amount, phoneNumber } = request.data as {
      tenantId: string;
      unitId: string;
      amount: number;
      phoneNumber: string;
    };

    const config = await getPropertyConfig();

    const paymentRef = db.collection("payments").doc();
    await paymentRef.set({
      id: paymentRef.id,
      tenantId,
      unitId,
      type: "RENT",
      amount,
      phoneNumber,
      status: "PENDING",
      rentPaybill: config.rentPaybill,
      rentAccount: `${config.rentAccountPrefix}-${unitId}`,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      paymentId: paymentRef.id,
      rentPaybill: config.rentPaybill,
      rentAccount: `${config.rentAccountPrefix}-${unitId}`,
    };
  }
);

export const initiateKplcPayment = functions.onCall(
  { secrets: [payHeroUsername, payHeroPassword] },
  async (request) => {
    const { tenantId, meterNumber, amount, phoneNumber } = request.data as {
      tenantId: string;
      meterNumber: string;
      amount: number;
      phoneNumber: string;
    };

    const paymentRef = db.collection("payments").doc();
    await paymentRef.set({
      id: paymentRef.id,
      tenantId,
      type: "KPLC_TOKEN",
      amount,
      phoneNumber,
      meterNumber,
      status: "PENDING",
      kplcPaybill: "888880",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      paymentId: paymentRef.id,
      kplcPaybill: "888880",
      meterNumber,
    };
  }
);

export const initiateInternetPayment = functions.onCall(
  { secrets: [payHeroUsername, payHeroPassword] },
  async (request) => {
    const { tenantId, amount, phoneNumber } = request.data as {
      tenantId: string;
      amount: number;
      phoneNumber: string;
    };

    const tenantDoc = await db.collection("tenants").doc(tenantId).get();
    const tenant = tenantDoc.data() as { name: string } | undefined;
    const config = await getPropertyConfig();

    const paymentRef = db.collection("payments").doc();
    await paymentRef.set({
      id: paymentRef.id,
      tenantId,
      type: "INTERNET",
      amount,
      phoneNumber,
      tillNumber: config.internetTillNumber,
      status: "PENDING",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    const callbackUrl = `https://<YOUR-REGION>-<YOUR-PROJECT-ID>.cloudfunctions.net/payheroCallback`;

    const { reference, checkoutRequestId } = await callPayHeroSTK({
      amount,
      phoneNumber,
      channelId: config.payHeroChannelId,
      externalReference: paymentRef.id,
      customerName: tenant?.name ?? "Tenant",
      callbackUrl,
      username: payHeroUsername.value(),
      password: payHeroPassword.value(),
    });

    await paymentRef.update({ payHeroReference: reference, checkoutRequestId });

    return { paymentId: paymentRef.id };
  }
);

export const payheroCallback = functions.onRequest(async (req, res) => {
  if (req.method !== "POST") { res.status(405).send("Method Not Allowed"); return; }

  const body = req.body as {
    status: string;
    reference: string;
    MpesaReceiptNumber?: string;
    external_reference: string;
  };

  const paymentId = body.external_reference;
  if (!paymentId) { res.status(400).send("Missing external_reference"); return; }

  const paymentRef = db.collection("payments").doc(paymentId);

  if (body.status === "SUCCESS") {
    await paymentRef.update({
      status: "COMPLETED",
      mpesaReceiptNumber: body.MpesaReceiptNumber ?? "",
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else {
    const status = body.status === "CANCELLED" ? "CANCELLED" : "FAILED";
    await paymentRef.update({ status });
  }

  res.status(200).json({ received: true });
});
