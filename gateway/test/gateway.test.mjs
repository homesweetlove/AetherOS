import { test } from "node:test";
import assert from "node:assert/strict";
import { createGateway, sanitizePlan } from "../server.mjs";

const key = "unit-test-key";
const secret = "a-test-secret-with-more-than-32-chars";
const clean = { reply: "확인해 주세요", actions: [{ type: "message", channel: "kakao", recipient: "엄마", body: "늦어", app: "", setting: "" }] };

test("rejects unsupported actions and invalid recipients", () => {
  assert.deepEqual(sanitizePlan(clean), clean);
  assert.throws(() => sanitizePlan({ reply: "x", actions: [{type:"shell"}] }));
  assert.throws(() => sanitizePlan({ reply: "x", actions: [{...clean.actions[0], recipient: ""}] }));
  assert.throws(() => sanitizePlan({ reply: "", actions: Array(5).fill(clean.actions[0]) }));
});
test("requires auth and uses Responses API without persistent storage", async () => {
  let upstream;
  const server = createGateway({
    apiKey: key, deviceToken: secret, fetcher: async (_url, options) => {
      upstream = JSON.parse(options.body);
      return { ok: true, json: async () => ({status:"completed", output:[{content:[{type:"output_text", text:JSON.stringify(clean)}]}]}) };
    }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try {
    const url = `http://127.0.0.1:${server.address().port}/v1/plan`;
    const denied = await fetch(url, {method:"POST", headers:{"content-type":"application/json"}, body: JSON.stringify({prompt:"안녕"})});
    assert.equal(denied.status, 401);
    const ok = await fetch(url, {method:"POST", headers:{"content-type":"application/json", authorization:`Bearer ${secret}`}, body:JSON.stringify({prompt:"엄마한테 카톡 보내줘"})});
    assert.equal(ok.status, 200);
    assert.deepEqual(await ok.json(), clean);
    assert.equal(upstream.store, false);
    assert.equal(upstream.text.format.type, "json_schema");
  } finally { await new Promise(resolve => server.close(resolve)); }
});
