/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * This package defines the values that can appear in the columns
 *
 * <ul>
 *   <li>methods.annotation
 *   <li>method_locations.annotation
 *   <li>packages.annotation
 *   <li>types.annotation
 * </ul>
 *
 * <p>They are stored in the database in JSON format.
 *
 * <p>The annotations are used for presentation purposes on the web interface as well as in reports.
 *
 * <p>They are maintained by a user that wants to suppress false positives, i.e., tell Codekvast
 * that a method/type/package/package_location should not be reported as dead, even if it has not
 * been invoked for a certain period.
 *
 * <p>Example: exception handlers, code that is already planned to be removed.
 *
 * @author olle.hallin@crisp.se
 */
package io.codekvast.dashboard.dashboard.model.annotations;
