// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//

#ifndef ENT_SRC_YB_UTIL_CDC_TEST_UTIL_H
#define ENT_SRC_YB_UTIL_CDC_TEST_UTIL_H

#include "yb/common/wire_protocol.h"
#include "yb/cdc/cdc_service.pb.h"
#include "yb/cdc/cdc_service.proxy.h"
#include "yb/rpc/rpc_controller.h"

namespace yb {
namespace cdc {

void AssertIntKey(const google::protobuf::RepeatedPtrField<cdc::KeyValuePairPB>& key,
                  int32_t value);

void CreateCDCStream(const std::unique_ptr<CDCServiceProxy>& cdc_proxy,
                     const TableId& table_id,
                     CDCStreamId* stream_id);

} // namespace cdc
} // namespace yb

#endif // ENT_SRC_YB_UTIL_CDC_TEST_UTIL_H
