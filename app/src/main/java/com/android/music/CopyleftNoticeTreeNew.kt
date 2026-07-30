/*
 * Copyright (c) 2026 Auxio Project
 * CopyleftNoticeTreeNew.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.android.music

import timber.log.Timber

class CopyleftNoticeTreeNew : Timber.DebugTree() {
    // Feel free to remove this logger if you are forking the project in good faith.
    //
    // However, if you are stealing this source code, wrapping it in a new coat of paint, and
    // releasing it as your own closed-source app, understand that you are not building a product -
    // you are taking someone else's work and trying to extract a quick payout from it.
    //
    // There are better ways to spend your time than copying the work of others just to chase a few
    // pennies in ad revenue. Create something meaningful, contribute to the community, and respect
    // the developers whose work made yours possible.
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(
            priority,
            tag,
            "Hey! Music is an open-source fork of the Auxio open-source project licensed under the GPLv3 license!" +
                "You can fork this project and even add ads, but it still needs to be kept open-source with the same license!",
            t,
        )
    }
}
