/*                         ADLINK Edge SDK
 *
 *   This software and documentation are Copyright 2018 to 2020 ADLINK
 *   Technology Limited, its affiliated companies and licensors. All rights
 *   reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

/**
 * This is a simple throughput application measuring obtainable throughput using the thingapi
 *
 */

package writer;

// Writer mode:
//  STANDARD = use default write function
//  OUTPUT_HANDLER = use output handler for writing
//  OUTPUT_HANDLER_NOT_THREAD_SAFE = use non-thread-safe write method for output handler
public enum WriterMode {
    STANDARD,
    OUTPUT_HANDLER,
    OUTPUT_HANDLER_NOT_THREAD_SAFE
}
