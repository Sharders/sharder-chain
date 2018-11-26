/******************************************************************************
 * Copyright © 2017 sharder.org.                             *
 * Copyright © 2014-2017 ichaoj.com.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,*
 * no part of the COS software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

QUnit.module("nxt.address");

QUnit.test("nxtAddress", function (assert) {
    var address = new NxtAddress();
    assert.equal(address.set("SSA-XK4R-7VJU-6EQG-7R335"), true, "valid address");
    assert.equal(address.toString(), "SSA-XK4R-7VJU-6EQG-7R335", "address");
    assert.equal(address.set("SSA-XK4R-7VJU-6EQG-7R336"), false, "invalid address");
});
