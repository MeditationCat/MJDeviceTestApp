// ICommandResultAidl.aidl
package com.zhi_tech.magiceyessdk;

// Declare any non-default types here with import statements

interface ICommandResultAidl {
    void OnCommandResultChanged(int cmd, in byte[] data, int length);
}
