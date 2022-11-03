package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.UpdateType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListUpdateWrapper<T> {
    private UpdateType type;
    private T object;

    private String group;

    public static <T> ListUpdateWrapper<T> of(UpdateType type, T object){
        final ListUpdateWrapper<T> wrapper = new ListUpdateWrapper<>();
        wrapper.type = type;
        wrapper.object = object;
        return wrapper;
    }

    public static <T> ListUpdateWrapper<T> of(UpdateType type, T object, String group){
        final ListUpdateWrapper<T> wrapper = new ListUpdateWrapper<>();
        wrapper.type = type;
        wrapper.object = object;
        wrapper.group = group;
        return wrapper;
    }
}
