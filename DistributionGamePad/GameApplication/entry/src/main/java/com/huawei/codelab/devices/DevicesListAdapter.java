/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.codelab.devices;

import com.huawei.codelab.ResourceTable;

import ohos.agp.colors.RgbPalette;
import ohos.agp.components.AbsButton;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.ComponentState;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.components.element.StateElement;
import ohos.app.Context;
import ohos.distributedschedule.interwork.DeviceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 设备列表适配器
 *
 * @since 2021-01-11
 */
public class DevicesListAdapter extends BaseItemProvider {
    private static final int FOUR = 4;
    private List<DeviceInfo> deviceInfoList;
    private Context context;
    private List<Integer> selectPositions = new ArrayList<>();

    /**
     * 设备列表适配器 构造函数
     *
     * @param listBasicInfo listBasicInfo
     * @param context context
     * @since 2021-01-11
     */
    public DevicesListAdapter(List<DeviceInfo> listBasicInfo, Context context) {
        this.deviceInfoList = listBasicInfo;
        this.context = context;
    }

    @Override
    public int getCount() {
        return deviceInfoList == null ? 0 : deviceInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return Optional.of(deviceInfoList.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Component getComponent(int position, Component component, ComponentContainer componentContainer) {
        ViewHolder viewHolder = null;
        Component mComponent = component;
        if (mComponent == null) {
            mComponent = LayoutScatter.getInstance(context).parse(ResourceTable.Layout_item_device_list, null, false);
            viewHolder = new ViewHolder();
            if (mComponent.findComponentById(ResourceTable.Id_device_name) instanceof Checkbox) {
                viewHolder.devicesName = (Checkbox) mComponent.findComponentById(ResourceTable.Id_device_name);
                viewHolder.devicesName.setButtonElement(viewHolder.getCheckElement());
                viewHolder.devicesName.setCheckedStateChangedListener(new AbsButton.CheckedStateChangedListener() {
                    @Override
                    public void onCheckedChanged(AbsButton absButton, boolean isSelected) {
                        if (isSelected) {
                            selectPositions.add(new Integer(position));
                        } else {
                            selectPositions.remove(new Integer(position));
                        }
                    }
                });
            }
            if (mComponent.findComponentById(ResourceTable.Id_device_id) instanceof Text) {
                viewHolder.devicesId = (Text) mComponent.findComponentById(ResourceTable.Id_device_id);
            }
            mComponent.setTag(viewHolder);
        } else {
            if (mComponent.getTag() instanceof ViewHolder) {
                viewHolder = (ViewHolder) mComponent.getTag();
            }
        }
        if (viewHolder != null) {
            viewHolder.devicesName.setText(deviceInfoList.get(position).getDeviceName());
            String deviceId = deviceInfoList.get(position).getDeviceId();
            deviceId = deviceId.substring(0, FOUR) + "****** " + deviceId.substring(deviceId.length() - FOUR);
            viewHolder.devicesId.setText(deviceId);
        }
        return mComponent;
    }

    /**
     * 设备列表（设备名、设备ID）
     *
     * @return selectDeviceInfos
     */
    public List<DeviceInfo> getSelectDevices() {
        List<DeviceInfo> selectDeviceInfos = new ArrayList<>(deviceInfoList.size());
        for (int selectPosition: selectPositions) {
            selectDeviceInfos.add(deviceInfoList.get(selectPosition));
        }
        return selectDeviceInfos;
    }

    /**
     * 设备列表（设备名、设备ID）
     *
     * @since 2021-01-11
     */
    private static class ViewHolder {
        private Checkbox devicesName;
        private Text devicesId;

        /**
         * getCheckElement
         *
         * @return checkElement
         * @since 2021-01-11
         */
        public static StateElement getCheckElement() {
            ShapeElement elementButtonOn = new ShapeElement();
            elementButtonOn.setRgbColor(RgbPalette.GREEN);
            ShapeElement elementButtonOff = new ShapeElement();
            elementButtonOff.setRgbColor(RgbPalette.BLACK);
            StateElement checkElement = new StateElement();
            checkElement.addState(new int[]{ComponentState.COMPONENT_STATE_CHECKED}, elementButtonOn);
            checkElement.addState(new int[]{ComponentState.COMPONENT_STATE_EMPTY}, elementButtonOff);

            return checkElement;
        }
    }
}
